package com.cap.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"default", "dev"})
@Slf4j
public class DevEmailService implements EmailService {

    @Override
    public String sendPasswordResetToken(String email, String token) {
        log.info("═══════════════════════════════════════════════════════");
        log.info("PASSWORD RESET TOKEN (DEV MODE)");
        log.info("Email: {}", email);
        log.info("Token: {}", token);
        log.info("═══════════════════════════════════════════════════════");
        return token; // return token so frontend can display it
    }
}
