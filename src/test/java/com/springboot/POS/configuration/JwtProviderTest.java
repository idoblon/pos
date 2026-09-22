package com.springboot.POS.configuration;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtProviderTest {

    private static final String SECRET =
            "unit-test-secret-key-0123456789012345678901234567890123456789";

    private final JwtProvider provider = new JwtProvider();

    @BeforeEach
    void setSecret() {
        // JwtConstant.JWT_SECRET is a settable static, normally wired by @Value.
        JwtConstant.JWT_SECRET = SECRET;
    }

    private String signedToken(String email, long expiryMillis) {
        return Jwts.builder()
                .issuedAt(new Date(expiryMillis - 86_400_000L))
                .expiration(new Date(expiryMillis))
                .claim("email", email)
                .claim("authorities", "ROLE_CASHIER")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }

    @Test
    void generateAndParseRoundTrip() {
        var auth = new UsernamePasswordAuthenticationToken(
                "cashier@example.com", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_BRANCH_CASHIER")));

        String token = provider.generateToken(auth);

        // Contract: getEmailFromToken expects the raw Authorization header value.
        assertEquals("cashier@example.com", provider.getEmailFromToken("Bearer " + token));
        assertEquals("cashier@example.com", provider.getEmailFromRefreshToken(token));
    }

    @Test
    void accessTokenParserExpectsAuthorizationHeaderValue() {
        var auth = new UsernamePasswordAuthenticationToken(
                "a@b.c", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        String token = provider.generateToken(auth);

        assertEquals("a@b.c", provider.getEmailFromToken("Bearer " + token));
        // Without the "Bearer " prefix the fixed substring(7) corrupts the token.
        assertThrows(io.jsonwebtoken.JwtException.class, () -> provider.getEmailFromToken(token));
    }

    @Test
    void refreshFlowAcceptsBearerPrefixedToken() {
        var auth = new UsernamePasswordAuthenticationToken(
                "a@b.c", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        String token = provider.generateToken(auth);
        assertEquals("a@b.c", provider.getEmailFromRefreshToken("Bearer " + token));
    }

    @Test
    void refreshAcceptsTokenExpiredTwoDaysAgo() {
        long expiredTwoDaysAgo = System.currentTimeMillis() - 2L * 86_400_000;
        String token = signedToken("late@example.com", expiredTwoDaysAgo);
        assertEquals("late@example.com", provider.getEmailFromRefreshToken(token));
    }

    @Test
    void refreshRejectsTokenExpiredBeyondSevenDays() {
        long expiredEightDaysAgo = System.currentTimeMillis() - 8L * 86_400_000;
        String token = signedToken("too-old@example.com", expiredEightDaysAgo);
        assertThrows(ExpiredJwtException.class,
                () -> provider.getEmailFromRefreshToken(token));
    }

    @Test
    void accessTokenRejectsExpiredToken() {
        long expiredYesterday = System.currentTimeMillis() - 86_400_000;
        String token = signedToken("late@example.com", expiredYesterday);
        assertThrows(ExpiredJwtException.class,
                () -> provider.getEmailFromToken("Bearer " + token));
    }

    @Test
    void tokenSignedWithDifferentKeyRejected() {
        String forged = Jwts.builder()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .claim("email", "attacker@example.com")
                .signWith(Keys.hmacShaKeyFor(
                        "a-completely-different-attacker-key-0123456789abcdef".getBytes()))
                .compact();
        assertThrows(Exception.class, () -> provider.getEmailFromRefreshToken(forged));
    }
}
