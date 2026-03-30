package com.cap.identity.util;

import com.cap.identity.entity.User;
import com.cap.identity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890";
    private final long expiryMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiryMs", expiryMs);
    }

    @Test
    void generateToken_Success() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .role(Role.EMPLOYEE)
                .build();

        String token = jwtUtil.generateToken(user);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void extractUserId_Success() {
        User user = User.builder()
                .id(123L)
                .email("test@test.com")
                .role(Role.EMPLOYEE)
                .build();

        String token = jwtUtil.generateToken(user);
        String userId = jwtUtil.extractUserId(token);

        assertEquals("123", userId);
    }

    @Test
    void extractRole_Success() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .role(Role.ADMIN)
                .build();

        String token = jwtUtil.generateToken(user);
        String role = jwtUtil.extractRole(token);

        assertEquals("ADMIN", role);
    }

    @Test
    void extractEmail_Success() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .role(Role.EMPLOYEE)
                .build();

        String token = jwtUtil.generateToken(user);
        String email = jwtUtil.extractEmail(token);

        assertEquals("test@test.com", email);
    }

    @Test
    void isTokenExpired_False() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .role(Role.EMPLOYEE)
                .build();

        String token = jwtUtil.generateToken(user);
        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void validateToken_True() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .role(Role.EMPLOYEE)
                .build();

        String token = jwtUtil.generateToken(user);
        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_False_Malformed() {
        boolean isValid = jwtUtil.validateToken("malformed-token");
        assertFalse(isValid);
    }

    @Test
    void validateToken_False_Expired() {
        // Set very short expiry
        ReflectionTestUtils.setField(jwtUtil, "expiryMs", -100L);
        
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .role(Role.EMPLOYEE)
                .build();

        String token = jwtUtil.generateToken(user);
        boolean isValid = jwtUtil.validateToken(token);

        assertFalse(isValid);
    }
}
