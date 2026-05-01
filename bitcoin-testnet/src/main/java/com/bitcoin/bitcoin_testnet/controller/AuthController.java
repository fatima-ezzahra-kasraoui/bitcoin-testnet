package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.dto.AuthRequest;
import com.bitcoin.bitcoin_testnet.dto.MfaSetupResponse;
import com.bitcoin.bitcoin_testnet.dto.MfaVerifyRequest;
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

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean available = userRepository.findByUsername(username).isEmpty();
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        return ResponseEntity.ok(authService.generateTotpSecret(username));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody MfaVerifyRequest request) {
        String token = authHeader.substring(7);
        if (!jwtService.isPreAuthToken(token)) {
            return ResponseEntity
                .status(403)
                .body(Map.of("message", "Invalid token type. Use your pre-auth token."));
        }
        String username = jwtService.extractUsername(token);
        return ResponseEntity.ok(authService.verifyTotp(username, request.getCode()));
    }

    // Returns current MFA status for the logged-in user
    @GetMapping("/mfa/status")
    public ResponseEntity<Map<String, Object>> getMfaStatus(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        return ResponseEntity.ok(authService.getMfaStatus(username));
    }

    // Enable MFA — requires password confirmation
    @PostMapping("/mfa/enable")
    public ResponseEntity<Map<String, Object>> enableMfa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        return ResponseEntity.ok(authService.enableMfa(username, request.get("password")));
    }

    // Disable MFA — requires password confirmation
    @PostMapping("/mfa/disable")
    public ResponseEntity<Map<String, String>> disableMfa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        return ResponseEntity.ok(authService.disableMfa(username, request.get("password")));
    }
}