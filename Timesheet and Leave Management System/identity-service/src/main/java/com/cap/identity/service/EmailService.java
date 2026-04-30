package com.cap.identity.service;

public interface EmailService {
    String sendPasswordResetToken(String email, String token);
}
