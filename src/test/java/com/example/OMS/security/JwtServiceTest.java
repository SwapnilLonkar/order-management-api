package com.example.OMS.security;

import com.example.OMS.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    // HS256 needs a key of at least 256 bits (32 bytes).
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256";
    private static final long EXPIRATION = 86400000L; // 24 h

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    void generateTokenThenExtractUsernameReturnsSubject() {
        String token = jwtService.generateToken("alice", Role.CUSTOMER);

        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    void generateTokenStoresRoleClaim() {
        String token = jwtService.generateToken("alice", Role.SELLER);

        assertEquals("SELLER", jwtService.extractRole(token));
    }

    @Test
    void isTokenValidReturnsTrueForMatchingUsername() {
        String token = jwtService.generateToken("alice", Role.CUSTOMER);

        assertTrue(jwtService.isTokenValid(token, "alice"));
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUsername() {
        String token = jwtService.generateToken("alice", Role.CUSTOMER);

        assertFalse(jwtService.isTokenValid(token, "bob"));
    }

    @Test
    void isTokenExpiredReturnsFalseForFreshToken() {
        String token = jwtService.generateToken("alice", Role.CUSTOMER);

        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void expiredTokenIsRejected() {
        // A negative expiration makes the token expire before it is issued.
        // jjwt rejects expired tokens at parse time, so any claim read throws
        // ExpiredJwtException instead of returning a value.
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken("alice", Role.CUSTOMER);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(token));
        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenExpired(token));
        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(token, "alice"));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("alice", Role.CUSTOMER);
        String tampered = token.substring(0, token.length() - 1)
                + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        assertThrows(JwtException.class, () -> jwtService.extractUsername(tampered));
    }
}
