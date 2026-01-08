package com.workoutplanner.service;

import com.workoutplanner.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "mySecretKey123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

        testUser = new User("testuser", "test@example.com", "password123");
    }

    @Test
    public void testGenerateToken() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    public void testExtractUsername() {
        String token = jwtService.generateToken(testUser);
        String extractedUsername = jwtService.extractUsername(token);

        assertEquals(testUser.getUsername(), extractedUsername);
    }

    @Test
    public void testIsTokenValid() {
        String token = jwtService.generateToken(testUser);
        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertTrue(isValid);
    }

    @Test
    public void testValidateToken() {
        String token = jwtService.generateToken(testUser);
        Boolean isValid = jwtService.validateToken(token, testUser.getUsername());

        assertTrue(isValid);
    }

    @Test
    public void testGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 0);

        // Refresh token should be valid
        assertTrue(jwtService.isTokenValid(refreshToken, testUser));
    }

    @Test
    public void testGetUsernameFromToken() {
        String token = jwtService.generateToken(testUser);
        String username = jwtService.getUsernameFromToken(token);

        assertEquals(testUser.getUsername(), username);
    }

    @Test
    public void testGetExpirationDateFromToken() {
        String token = jwtService.generateToken(testUser);
        java.util.Date expirationDate = jwtService.getExpirationDateFromToken(token);

        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new java.util.Date()));
    }
}