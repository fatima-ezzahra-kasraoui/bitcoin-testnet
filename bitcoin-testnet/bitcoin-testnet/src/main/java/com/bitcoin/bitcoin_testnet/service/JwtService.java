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

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    // Pre-auth token issued after password check when MFA is enabled.
    // Has mfa_pending claim, expires in 5 minutes, only usable on /mfa/verify.
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