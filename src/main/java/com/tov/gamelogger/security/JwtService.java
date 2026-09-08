package com.tov.gamelogger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl:15m}") Duration accessTokenTtl,
            @Value("${jwt.refresh-token-ttl:30d}") Duration refreshTokenTtl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String generateAccessToken(Long userId, String email) {
        return buildToken(userId, email, TYPE_ACCESS, accessTokenTtl);
    }

    public String generateRefreshToken(Long userId, String email) {
        return buildToken(userId, email, TYPE_REFRESH, refreshTokenTtl);
    }

    public Claims parseAccessToken(String token) {
        return parseAndValidateType(token, TYPE_ACCESS);
    }

    public Claims parseRefreshToken(String token) {
        return parseAndValidateType(token, TYPE_REFRESH);
    }

    public static Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public static String extractEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    private String buildToken(Long userId, String email, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    private Claims parseAndValidateType(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Unexpected token type");
        }
        return claims;
    }
}
