package com.cap.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cap.identity.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    public PasswordResetToken findByTokenAndUsedFalseAndExpiryDateAfter(String token,LocalDateTime now);
}