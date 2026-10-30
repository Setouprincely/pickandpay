package com.doch.User.service.interfaces;

import com.doch.User.dto.Request.*;
import com.doch.User.model.User;
import org.springframework.http.ResponseEntity;

import com.doch.User.dto.Request.AuthRequest;
import com.doch.User.dto.Request.SellerRegistration;
import com.doch.User.dto.Request.UserRegistration;
import com.doch.User.dto.Request.VerifyUserDTO;


public interface AuthService {
    Object createAccount(UserRegistration userRegistration);
    Object createSellerAccount(SellerRegistration userRegistration);
    ResponseEntity<?> login(AuthRequest authRequest);
    void logout(String accessToken);
    boolean verifyUser(VerifyUserDTO verifyUserDTO);
    void resentVerificationEmail(String mail);
    String onboardStaff(StaffOnboardingRequest request, String roleName);
    String completeStaffSetup(StaffSetupCompletionRequest request);
    void resendStaffInvitation(String email);
    ResponseEntity<?> getUserDetails(String email);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(User currentUser, ChangePasswordRequest request);
}
