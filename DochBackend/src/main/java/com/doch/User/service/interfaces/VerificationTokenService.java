package com.doch.User.service.interfaces;


import com.doch.User.model.User;
import com.doch.User.model.VerificationToken;

public interface VerificationTokenService {
    VerificationToken createVerificationToken(User user);
    VerificationToken createVerificationToken(User user, long validMinutes);
    VerificationToken getVerificationToken(String email, String token);
    VerificationToken getVerificationTokenByUser(User user);
    boolean verifyOtp(String otpCode, String email);
}
