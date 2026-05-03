package com.bitcoin.bitcoin_testnet.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.bitcoin.bitcoin_testnet.dto.MfaSetupResponse;
import com.bitcoin.bitcoin_testnet.model.User;
import com.bitcoin.bitcoin_testnet.repository.UserRepository;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EncryptionService encryptionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Rate limiting — tracks failed MFA attempts per username
    private final ConcurrentHashMap<String, long[]> mfaAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    public Map<String, Object> register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already taken");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setMfaEnabled(false);
        userRepository.save(user);

        MfaSetupResponse mfaSetup = generateTotpSecret(username);
        String preAuthToken = jwtService.generatePreAuthToken(username);

        return Map.of(
            "token", preAuthToken,
            "qrCodeUrl", mfaSetup.getQrCodeUrl(),
            "secret", mfaSetup.getSecret()
        );
    }

    public MfaSetupResponse generateTotpSecret(String username) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA1");
            keyGenerator.init(160);
            SecretKey secret = keyGenerator.generateKey();

            Base32 base32 = new Base32();
            String base32Secret = base32.encodeToString(secret.getEncoded());

            String encryptedSecret = encryptionService.encrypt(base32Secret);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setTotpSecret(encryptedSecret);
            userRepository.save(user);

            String otpauthUri = String.format(
                "otpauth://totp/BitcoinTestNet:%s?secret=%s&issuer=BitcoinTestNet&algorithm=SHA1&digits=6&period=30",
                username, base32Secret
            );

            MfaSetupResponse response = new MfaSetupResponse();
            response.setQrCodeUrl(otpauthUri);
            response.setSecret(base32Secret);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP secret", e);
        }
    }

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.isMfaEnabled()) {
            String preAuthToken = jwtService.generatePreAuthToken(username);
            return Map.of(
                "mfaRequired", true,
                "preAuthToken", preAuthToken
            );
        }

        String token = jwtService.generateToken(username);
        return Map.of("token", token);
    }

    public Map<String, String> verifyTotp(String username, String code) {
        checkRateLimit(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTotpSecret() == null) {
            throw new RuntimeException("MFA not set up for this user");
        }

        String base32Secret = encryptionService.decrypt(user.getTotpSecret());
        Base32 base32 = new Base32();
        byte[] secretBytes = base32.decode(base32Secret);

        try {
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
            SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA1");

            Instant now = Instant.now();
            boolean valid = false;

            // Check ±30s windows to handle clock drift
            for (int delta = -1; delta <= 1; delta++) {
                Instant window = now.plusSeconds(delta * 30L);
                int generatedCode = totp.generateOneTimePassword(secretKey, window);
                if (String.format("%06d", generatedCode).equals(code)) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                recordFailedAttempt(username);
                throw new RuntimeException("Invalid or expired code");
            }

            mfaAttempts.remove(username);

            if (!user.isMfaEnabled()) {
                user.setMfaEnabled(true);
                userRepository.save(user);
            }

            String token = jwtService.generateToken(username);
            return Map.of("token", token);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("TOTP verification error", e);
        }
    }

    public Map<String, Object> getMfaStatus(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Map.of(
            "mfaEnabled", user.isMfaEnabled(),
            "hasTotpSecret", user.getTotpSecret() != null
        );
    }

    public Map<String, Object> enableMfa(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (user.getTotpSecret() == null) {
            MfaSetupResponse setup = generateTotpSecret(username);
            return Map.of(
                "mfaEnabled", false,
                "requiresSetup", true,
                "qrCodeUrl", setup.getQrCodeUrl(),
                "secret", setup.getSecret()
            );
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
        return Map.of("mfaEnabled", true, "message", "MFA enabled successfully");
    }

    public Map<String, String> disableMfa(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        user.setMfaEnabled(false);
        userRepository.save(user);
        return Map.of("message", "MFA disabled successfully");
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Le mot de passe doit avoir au moins 8 caractères");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void checkRateLimit(String username) {
        long[] attempts = mfaAttempts.get(username);
        if (attempts == null) return;

        long lockUntil = attempts[1];
        if (lockUntil > System.currentTimeMillis()) {
            long secondsLeft = (lockUntil - System.currentTimeMillis()) / 1000;
            throw new RuntimeException("Too many failed attempts. Try again in " + secondsLeft + " seconds.");
        }
        if (lockUntil > 0 && lockUntil <= System.currentTimeMillis()) {
            mfaAttempts.remove(username);
        }
    }

    private void recordFailedAttempt(String username) {
        long[] attempts = mfaAttempts.getOrDefault(username, new long[]{0, 0});
        attempts[0]++;
        if (attempts[0] >= MAX_ATTEMPTS) {
            attempts[1] = System.currentTimeMillis() + LOCK_DURATION_MS;
        }
        mfaAttempts.put(username, attempts);
    }
}
