package com.cap.identity.entity;

import com.cap.identity.enums.Role;
import com.cap.identity.enums.Status;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void userBuilder_And_Getters_Success() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("hash")
                .fullName("Test User")
                .employeeCode("EMP01")
                .role(Role.EMPLOYEE)
                .status(Status.ACTIVE)
                .failedLoginCount(0)
                .isDeleted(false)
                .createdBy("admin")
                .lastLoginAt(now)
                .build();
        
        user.setCreatedOn(now);
        user.setUpdatedOn(now);

        assertEquals(1L, user.getId());
        assertEquals("test@test.com", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals("Test User", user.getFullName());
        assertEquals("EMP01", user.getEmployeeCode());
        assertEquals(Role.EMPLOYEE, user.getRole());
        assertEquals(Status.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedLoginCount());
        assertFalse(user.getIsDeleted());
        assertEquals(now, user.getLastLoginAt());
        assertEquals(now, user.getCreatedOn());
        assertEquals(now, user.getUpdatedOn());
        assertEquals("admin", user.getCreatedBy());
    }

    @Test
    void userSetters_Success() {
        User user = new User();
        user.setId(2L);
        user.setEmail("new@test.com");

        assertEquals(2L, user.getId());
        assertEquals("new@test.com", user.getEmail());
    }
}
