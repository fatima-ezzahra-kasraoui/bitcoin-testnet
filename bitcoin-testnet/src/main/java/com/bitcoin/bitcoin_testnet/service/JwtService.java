package com.bitcoin.bitcoin_testnet.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final String SECRET = "bitcoin-testnet-secret-key-very-long-string-123456";
    private final long EXPIRATION = 86400000;        // 24 hours — full access token
    private final long PRE_AUTH_EXPIRATION = 300000; // 5 minutes — MFA pending token

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // Full JWT — issued after complete authentication (password + TOTP if MFA enabled)
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    // Pre-auth JWT — issued after password check when MFA is enabled
    // Has a special claim "mfa_pending: true" to mark it as incomplete
    // Only valid for 5 minutes and only usable on /mfa/verify
    public String generatePreAuthToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("mfa_pending", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + PRE_AUTH_EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Check if this is a pre-auth token (has mfa_pending claim)
    // Used in /mfa/verify to reject regular full tokens
    public boolean isPreAuthToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Boolean mfaPending = claims.get("mfa_pending", Boolean.class);
            return Boolean.TRUE.equals(mfaPending);
        } catch (Exception e) {
            return false;
        }
    }
}