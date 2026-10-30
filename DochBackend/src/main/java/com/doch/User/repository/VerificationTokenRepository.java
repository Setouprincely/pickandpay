package com.doch.User.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.doch.User.model.User;
import com.doch.User.model.VerificationToken;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {
    Optional<VerificationToken> findByUserEmailAndOtpCode(String email, String otpCode);
    Optional<VerificationToken> findByUser(User user);
    Optional<VerificationToken> findByOtpCode(String otpCode);
    Optional<VerificationToken> findByUserEmail(String email);
}
