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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EncryptionService encryptionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> register(String username, String password) {
        // Check if username is already taken
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        // Create and save the user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setMfaEnabled(false);
        userRepository.save(user);

        // Generate TOTP secret for this user
        MfaSetupResponse mfaSetup = generateTotpSecret(username);

        // Issue JWT token
        String token = jwtService.generateToken(username);

        // Return token + QR code info so frontend can show setup screen
        return Map.of(
            "token", token,
            "qrCodeUrl", mfaSetup.getQrCodeUrl(),
            "secret", mfaSetup.getSecret()
        );
    }

    public MfaSetupResponse generateTotpSecret(String username) {
        try {
            // Step 1 — Generate a random 160-bit secret key
            // 160 bits is the standard size for TOTP (RFC 6238)
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA1");
            keyGenerator.init(160);
            SecretKey secret = keyGenerator.generateKey();

            // Step 2 — Encode in Base32
            // Google Authenticator requires Base32 encoding (not Base64)
            // Base32 uses only A-Z and 2-7 — safe to type manually
            Base32 base32 = new Base32();
            String base32Secret = base32.encodeToString(secret.getEncoded());

            // Step 3 — Encrypt before storing in MongoDB
            // If the database is stolen, secrets are useless without the AES key
            String encryptedSecret = encryptionService.encrypt(base32Secret);

            // Step 4 — Save encrypted secret to the user document
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setTotpSecret(encryptedSecret);
            // mfaEnabled stays false until user confirms QR scan
            userRepository.save(user);

            // Step 5 — Build the otpauth URI
            // This is what gets encoded into the QR code
            // Format: otpauth://totp/LABEL?secret=SECRET&issuer=ISSUER
            String otpauthUri = String.format(
                "otpauth://totp/BitcoinTestNet:%s?secret=%s&issuer=BitcoinTestNet&algorithm=SHA1&digits=6&period=30",
                username,
                base32Secret
            );

            // Build and return the response
            MfaSetupResponse response = new MfaSetupResponse();
            response.setQrCodeUrl(otpauthUri);
            response.setSecret(base32Secret);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP secret", e);
        }
    }

    public Map<String, Object> login(String username, String password) {
        // Vague error — never reveal if username exists or password is wrong
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtService.generateToken(username);
        return Map.of("token", token);
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