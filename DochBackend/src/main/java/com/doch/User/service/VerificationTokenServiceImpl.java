package com.doch.User.service;

import com.doch.User.model.User;
import com.doch.User.model.VerificationToken;
// import com.doch.User.repository.UserRepository;
import com.doch.User.repository.VerificationTokenRepository;
import com.doch.User.service.interfaces.VerificationTokenService;
import com.doch.common.exceptions.DochErrorCode;
import com.doch.common.exceptions.TokenException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

     private final VerificationTokenRepository verificationTokenRepository;
    //  private final UserRepository userRepository;

    @Override
    public VerificationToken createVerificationToken(User user) {
        return createVerificationToken(user, 15);
    }

    @Override
    public VerificationToken createVerificationToken(User user, long validMinutes) {
        VerificationToken token = VerificationToken.builder()
                .otpCode(generateOTP(6))
                .createdTime(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(validMinutes))
                .isUsed(false)
                .user(user)
                .build();

        verificationTokenRepository.save(token);
        return token;
    }

    @Override
    public VerificationToken getVerificationToken(String email, String token) {
        // TODO Auto-generated method stub
        return verificationTokenRepository.findByUserEmailAndOtpCode(email, token)
                .orElseThrow(() -> new TokenException(DochErrorCode.OTP004));
    }

    @Override
    public VerificationToken getVerificationTokenByUser(User user) {
        // TODO Auto-generated method stub
        return verificationTokenRepository.findByUserEmail(user.getEmail()).orElseThrow(() -> new TokenException(DochErrorCode.OTP004));
    }

    @Override
    public boolean verifyOtp(String otpCode, String email) {
        VerificationToken token = verificationTokenRepository.findByUserEmailAndOtpCode(email, otpCode)
                .orElseThrow(() -> new TokenException(DochErrorCode.OTP001));

        if (token.isExpired()) {
            throw new TokenException(DochErrorCode.OTP002);
        }
        if (token.isUsed()) {
            throw new TokenException(DochErrorCode.OTP003);
        }

        token.markAsUsed();
        verificationTokenRepository.save(token);
        return true;

    }

    public String generateOTP(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10)); // Generates a digit (0-9)
        }
        return otp.toString();
    }
}
