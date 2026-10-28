package com.example.workflow.security;

import com.example.workflow.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    @DisplayName("JWT: Generates valid token with claims and extracts username and role correctly")
    void testTokenGenerationAndExtraction() {
        String token = jwtService.generateToken("testuser", Role.APPROVER);

        assertNotNull(token);
        assertEquals("testuser", jwtService.extractUsername(token));
        assertEquals(Role.APPROVER, jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token));
        assertEquals(expirationMs, jwtService.getExpirationMs());
    }

    @Test
    @DisplayName("JWT: Validates token against UserDetails")
    void testValidateTokenWithUserDetails() {
        String token = jwtService.generateToken("john_doe", Role.REQUESTER);
        UserDetails userDetails = new User("john_doe", "password", Collections.emptyList());

        assertTrue(jwtService.isTokenValid(token, userDetails));

        UserDetails differentUser = new User("jane_doe", "password", Collections.emptyList());
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    @DisplayName("JWT: Expired token or malformed token returns false")
    void testExpiredOrInvalidToken() {
        // Service with negative expiration for testing expired token
        JwtService expiredService = new JwtService(secret, -1000);
        String expiredToken = expiredService.generateToken("expiredUser", Role.REQUESTER);

        assertFalse(jwtService.isTokenValid(expiredToken));
        assertFalse(jwtService.isTokenValid("invalid.jwt.token.string"));
    }
}
