package com.tov.gamelogger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JwtServiceTest {

    private static final String SECRET = "6f1f4a9c2e7b4d3a8f0c5e6b1a2d3c4e5f60718293a4b5c6d7e8f90a1b2c3d4e";

    private final JwtService jwtService =
            new JwtService(SECRET, Duration.ofMinutes(15), Duration.ofDays(30));

    @Test
    void generatesAndParsesAccessToken() {
        String token = jwtService.generateAccessToken(42L, "alice@example.com");

        Claims claims = jwtService.parseAccessToken(token);

        assertThat(JwtService.extractUserId(claims)).isEqualTo(42L);
        assertThat(JwtService.extractEmail(claims)).isEqualTo("alice@example.com");
    }

    @Test
    void generatesAndParsesRefreshToken() {
        String token = jwtService.generateRefreshToken(7L, "bob@example.com");

        Claims claims = jwtService.parseRefreshToken(token);

        assertThat(JwtService.extractUserId(claims)).isEqualTo(7L);
        assertThat(JwtService.extractEmail(claims)).isEqualTo("bob@example.com");
    }

    @Test
    void rejectsRefreshTokenPresentedAsAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(1L, "user@example.com");

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> jwtService.parseAccessToken(refreshToken));
    }

    @Test
    void rejectsAccessTokenPresentedAsRefreshToken() {
        String accessToken = jwtService.generateAccessToken(1L, "user@example.com");

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> jwtService.parseRefreshToken(accessToken));
    }

    @Test
    void rejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minus(Duration.ofMinutes(1));
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("email", "user@example.com")
                .claim("type", "access")
                .issuedAt(Date.from(past.minus(Duration.ofMinutes(15))))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> jwtService.parseAccessToken(expiredToken));
    }

    @Test
    void rejectsTamperedSignature() {
        String token = jwtService.generateAccessToken(1L, "user@example.com");
        int lastDot = token.lastIndexOf('.');
        assertThat(lastDot).isPositive();
        int mutateIndex = lastDot + 1;
        char original = token.charAt(mutateIndex);
        char flipped = original == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, mutateIndex) + flipped + token.substring(mutateIndex + 1);

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> jwtService.parseAccessToken(tampered));
    }

    @Test
    void rejectsGarbageToken() {
        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> jwtService.parseAccessToken("not-a-jwt"));
    }
}
