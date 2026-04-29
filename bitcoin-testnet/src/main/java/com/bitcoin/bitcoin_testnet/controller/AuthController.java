package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.dto.AuthRequest;
import com.bitcoin.bitcoin_testnet.dto.MfaSetupResponse;
import com.bitcoin.bitcoin_testnet.repository.UserRepository;
import com.bitcoin.bitcoin_testnet.service.AuthService;
import com.bitcoin.bitcoin_testnet.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        authService.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // Check if username is available — used by register form in real time
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean available = userRepository.findByUsername(username).isEmpty();
        return ResponseEntity.ok(Map.of("available", available));
    }

    // Generate a new TOTP secret for the authenticated user
    // Called right after register to get the QR code
    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(
            @RequestHeader("Authorization") String authHeader) {

        // Extract username from the JWT token
        // authHeader looks like "Bearer eyJ..." so we remove the first 7 characters
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        return ResponseEntity.ok(authService.generateTotpSecret(username));
    }
}