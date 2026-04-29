package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.dto.MfaSetupResponse;
import com.bitcoin.bitcoin_testnet.model.User;
import com.bitcoin.bitcoin_testnet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base32;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EncryptionService encryptionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Rate limiting — tracks failed MFA attempts per username
    // Key: username, Value: [failedAttempts, lockUntilTimestamp]
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
        String token = jwtService.generateToken(username);

        return Map.of(
            "token", token,
            "qrCodeUrl", mfaSetup.getQrCodeUrl(),
            "secret", mfaSetup.getSecret()
        );
    }

    public MfaSetupResponse generateTotpSecret(String username) {
        try {
            // Generate random 160-bit secret
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA1");
            keyGenerator.init(160);
            SecretKey secret = keyGenerator.generateKey();

            // Encode in Base32 — required by Google Authenticator
            Base32 base32 = new Base32();
            String base32Secret = base32.encodeToString(secret.getEncoded());

            // Encrypt before storing in MongoDB
            String encryptedSecret = encryptionService.encrypt(base32Secret);

            // Save to user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setTotpSecret(encryptedSecret);
            userRepository.save(user);

            // Build otpauth URI for QR code
            String otpauthUri = String.format(
                "otpauth://totp/BitcoinTestNet:%s?secret=%s&issuer=BitcoinTestNet&algorithm=SHA1&digits=6&period=30",
                username,
                base32Secret
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

        // If MFA is enabled → return pre-auth token instead of full JWT
        if (user.isMfaEnabled()) {
            String preAuthToken = jwtService.generatePreAuthToken(username);
            return Map.of(
                "mfaRequired", true,
                "preAuthToken", preAuthToken
            );
        }

        // MFA not enabled → normal login, issue full JWT immediately
        String token = jwtService.generateToken(username);
        return Map.of("token", token);
    }

    public Map<String, String> verifyTotp(String username, String code) {
        // Step 1 — Check rate limiting
        checkRateLimit(username);

        // Step 2 — Get user and decrypt secret
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTotpSecret() == null) {
            throw new RuntimeException("MFA not set up for this user");
        }

        // Step 3 — Decrypt the stored secret
        String base32Secret = encryptionService.decrypt(user.getTotpSecret());

        // Step 4 — Decode Base32 back to bytes
        Base32 base32 = new Base32();
        byte[] secretBytes = base32.decode(base32Secret);

        // Step 5 — Verify the code
        try {
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
            SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA1");

            Instant now = Instant.now();
            boolean valid = false;

            // Check 3 windows: previous 30s, current 30s, next 30s
            // This handles clock drift between server and phone
            for (int delta = -1; delta <= 1; delta++) {
                Instant window = now.plusSeconds(delta * 30L);
                int generatedCode = totp.generateOneTimePassword(secretKey, window);
                if (String.format("%06d", generatedCode).equals(code)) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                // Record failed attempt for rate limiting
                recordFailedAttempt(username);
                throw new RuntimeException("Invalid or expired code");
            }

            // Step 6 — Code is valid
            // Reset failed attempts counter
            mfaAttempts.remove(username);

            // First successful verification activates MFA
            if (!user.isMfaEnabled()) {
                user.setMfaEnabled(true);
                userRepository.save(user);
            }

            // Issue the real full JWT
            String token = jwtService.generateToken(username);
            return Map.of("token", token);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("TOTP verification error", e);
        }
    }

    // --- Rate limiting helpers ---

    private void checkRateLimit(String username) {
        long[] attempts = mfaAttempts.get(username);
        if (attempts == null) return;

        long failedCount = attempts[0];
        long lockUntil = attempts[1];

        // Check if currently locked
        if (lockUntil > System.currentTimeMillis()) {
            long secondsLeft = (lockUntil - System.currentTimeMillis()) / 1000;
            throw new RuntimeException(
                "Too many failed attempts. Try again in " + secondsLeft + " seconds."
            );
        }

        // Lock expired — reset
        if (lockUntil > 0 && lockUntil <= System.currentTimeMillis()) {
            mfaAttempts.remove(username);
        }
    }

    private void recordFailedAttempt(String username) {
        long[] attempts = mfaAttempts.getOrDefault(username, new long[]{0, 0});
        attempts[0]++; // increment failed count

        if (attempts[0] >= MAX_ATTEMPTS) {
            // Lock the account for 15 minutes
            attempts[1] = System.currentTimeMillis() + LOCK_DURATION_MS;
        }

        mfaAttempts.put(username, attempts);
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}