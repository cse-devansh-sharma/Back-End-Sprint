package com.cap.identity.repository;


import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.cap.identity.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(
		    String tokenHash,
		    LocalDateTime now
		);
}