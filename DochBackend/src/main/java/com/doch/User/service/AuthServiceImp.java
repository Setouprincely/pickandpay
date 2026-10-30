package com.doch.User.service;

import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import com.doch.Payment.service.WalletService;
import com.doch.User.dto.AuthenticationResponse;
import com.doch.User.dto.Request.*;
import com.doch.User.dto.UserDetailResponse;
import com.doch.User.model.*;
import com.doch.User.repository.*;
import com.doch.User.service.interfaces.AuthService;
import com.doch.common.email.EmailService;
import com.doch.common.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService {

    // Injected property for the frontend URL (using DochMarket as default for auth flows)
    @Value("${application.frontend.market-base-url}")
    private String frontendBaseUrl;

    /**
     * DEV/STAGING FLAG: When true, newly registered accounts are enabled immediately
     * and no verification email is required. This unblocks testing when the email
     * provider (Resend) is not configured. MUST be false in production.
     */
    @Value("${application.auth.auto-verify-accounts:false}")
    private boolean autoVerifyAccounts;

    @Value("${application.auth.staff-invite-expiry-hours:72}")
    private long staffInviteExpiryHours;

    @Value("${application.frontend.driver-base-url:http://localhost:3001}")
    private String driverBaseUrl;

    @Value("${application.frontend.admin-base-url:http://localhost:3000}")
    private String adminBaseUrl;

    /**
     * The vehicle declared at onboarding becomes a fleet vehicle assigned to the driver, so dispatch can
     * assign it right away. An existing unassigned vehicle with the same plate is reused instead.
     */
    private void registerDriverVehicle(DriverProfile profile) {
        String plate = profile.getPlateNumber() == null ? "" : profile.getPlateNumber().trim();
        if (plate.isEmpty() || vehicleRepository.findByAssignedDriverId(profile.getId()).isPresent()) return;
        var byPlate = vehicleRepository.findByPlateNumber(plate);
        if (byPlate.isPresent()) {
            if (byPlate.get().getAssignedDriver() == null) {
                byPlate.get().setAssignedDriver(profile);
                vehicleRepository.save(byPlate.get());
            } else {
                log.warn("Vehicle {} is already assigned to another driver; not linked to {}", plate, profile.getId());
            }
            return;
        }
        com.doch.Fleet.model.VehicleCategory category = com.doch.Fleet.model.VehicleCategory.fromVehicleType(profile.getVehicleType());
        vehicleRepository.save(com.doch.Fleet.model.Vehicle.builder()
                .name(category.getLabel() + " " + plate)
                .vehicleType(profile.getVehicleType())
                .category(category)
                .plateNumber(plate)
                .status(com.doch.Fleet.model.VehicleStatus.ACTIVE)
                .assignedDriver(profile)
                .notes("Registered automatically from the driver's onboarding details.")
                .build());
        log.info("FLEET: registered {} ({}) for driver profile {}", plate, category, profile.getId());
    }

    /** Invitation links open the portal of the invited role and never carry the code (it is only in the email body). */
    private String staffSetupLink(User user, String roleName) {
        String base = "ROLE_DELIVERY".equals(roleName) ? driverBaseUrl : adminBaseUrl;
        return base + "/staff-setup/" + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8);
    }

    // Email failures must never roll back the onboarding transaction (sendEmailFromTemplate reports instead of throwing).
    private boolean sendStaffInvitationEmail(User user, String roleName, VerificationToken token, String setupLink) {
        Map<String, Object> context = Map.of(
                "name", user.getFirstName() != null ? user.getFirstName() : "",
                "role", "ROLE_DELIVERY".equals(roleName) ? "Driver" : roleName.replace("ROLE_", "").replace("_", " "),
                "otpCode", token.getOtpCode(),
                "expirationHours", staffInviteExpiryHours,
                "activationLink", setupLink
        );
        return emailService.sendEmailFromTemplate(user.getEmail(), "Join the DOCH Team", "staff-invitation", context);
    }

    @Override
    @Transactional
    public void resendStaffInvitation(String email) {
        userRepository.findByEmail(email)
                .filter(u -> !u.isEnabled())
                .filter(u -> u.getUserRole() != null && (u.getUserRole().getName().contains("ADMIN")
                        || "ROLE_DELIVERY".equals(u.getUserRole().getName())))
                .ifPresent(u -> {
                    String roleName = u.getUserRole().getName();
                    VerificationToken token = verificationTokenService.createVerificationToken(u, staffInviteExpiryHours * 60L);
                    sendStaffInvitationEmail(u, roleName, token, staffSetupLink(u, roleName));
                    log.info("Staff invitation re-sent to {}", u.getEmail());
                });
    }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final JWTUtils jwtUtils;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final VerificationTokenServiceImpl verificationTokenService;
    private final SellerRepository sellerRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final com.doch.Fleet.repository.VehicleRepository vehicleRepository;

    /** Roles one person may combine on a single account. Staff/admin accounts stay single-role. */
    private static final java.util.Set<String> COMBINABLE_ROLES = java.util.Set.of("ROLE_BUYER", "ROLE_SELLER", "ROLE_DELIVERY");

    /**
     * Adds {@code roleName} to an account that already uses this email. The caller must prove
     * ownership with the account's current password, so nobody can attach a role to someone else's account.
     */
    private User addRoleToExistingAccount(User existing, String password, String roleName) {
        if (existing.hasRole(roleName)) {
            throw new BusinessException("You already have a DOCH " + roleLabel(roleName)
                    + " account with this email. Sign in instead (or use 'Forgot password').");
        }
        if (!COMBINABLE_ROLES.containsAll(existing.getRoleNames())) {
            throw new BusinessException("This email belongs to a staff account and cannot be reused for another role.");
        }
        if (password == null || !passwordEncoder.matches(password, existing.getPassword())) {
            throw new BusinessException("A DOCH account with this email already exists. Enter the password of that account "
                    + "to add this role to it (or use 'Forgot password').");
        }
        Role role = roleRepository.findByNameAndIsDeletedFalse(roleName)
                .orElseThrow(() -> new AuthenticationException(DochErrorCode.AUTH002));
        existing.getExtraRoles().add(role);
        log.info("ROLE ADDED: {} now also holds {}", existing.getEmail(), roleName);
        return userRepository.save(existing);
    }

    private static String roleLabel(String roleName) {
        return switch (roleName) {
            case "ROLE_SELLER" -> "seller";
            case "ROLE_DELIVERY" -> "driver";
            default -> "customer";
        };
    }

    private Map<String, Object> roleAddedResponse(User user, String roleLabel, String sellerId) {
        boolean verificationRequired = !user.isEnabled();
        if (verificationRequired) sendActivationEmailSafely(user);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("existingAccount", true);
        body.put("verificationRequired", verificationRequired);
        body.put("roles", user.getRoleNames());
        if (sellerId != null) body.put("sellerId", sellerId);
        body.put("message", "Your existing DOCH account now also has " + roleLabel + " access. "
                + (verificationRequired ? "Verify your email, then sign in." : "Sign in with your existing password."));
        return body;
    }

    @Override
    @Transactional
    public Object createAccount(UserRegistration request) {
        // --- SECURITY BLOCK: Prevent self-registration as ADMIN or DRIVER ---
        String requestedRole = StringUtils.hasText(request.getRole()) ? request.getRole().toUpperCase() : "ROLE_BUYER";
        if (requestedRole.contains("ADMIN") || requestedRole.equals("ROLE_DELIVERY")) {
            log.warn("Security Alert: Unauthorized registration attempt as {} by email {}", requestedRole, request.getEmail());
            throw new BusinessException("Staff accounts must be created by a Super Admin.");
        }

        var existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            if (requestedRole.equals("ROLE_SELLER")) {
                throw new BusinessException("Use the Seller portal sign-up to add a store to your account.");
            }
            User user = addRoleToExistingAccount(existing.get(), request.getPassword(), requestedRole);
            walletService.getOrCreateWallet(user);
            return roleAddedResponse(user, "shopping", null);
        }

        Role role = roleRepository.findByNameAndIsDeletedFalse(requestedRole)
                .orElseThrow(() -> new AuthenticationException(DochErrorCode.AUTH002));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .userName(request.getUserName())
                .email(request.getEmail())
                .tel(request.getPhone())
                .userRole(role)
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(autoVerifyAccounts)
                .build();

        userRepository.save(user);

        if (autoVerifyAccounts) {
            log.warn("AUTO-VERIFY is ON: account {} enabled without email verification (dev/staging only).", user.getEmail());
            return "Account created and auto-verified (dev mode). You can log in immediately.";
        }

        sendActivationEmailSafely(user);
        return "Account created successfully. Please check your email to verify your account.";
    }

    @Override
    @Transactional
    public Object createSellerAccount(SellerRegistration request) {
        var existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            if (!sellerRepository.findByUserId(existingUser.get().getId()).isEmpty()) {
                throw new BusinessException("You already have a DOCH seller account with this email. Sign in instead (or use 'Forgot password').");
            }
            User user = addRoleToExistingAccount(existingUser.get(), request.getPassword(), "ROLE_SELLER");
            Seller seller = sellerRepository.save(Seller.builder()
                    .user(user)
                    .name(request.getStoreName())
                    .city(request.getCity())
                    .address(request.getAddress())
                    .rating(request.getRating())
                    .contactNumber(request.getContactNumber())
                    .build());
            walletService.getOrCreateWallet(user);
            notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT,
                    String.format("New seller registered: %s (%s) from %s. Verification pending.",
                            request.getStoreName(), request.getEmail(), request.getCity()));
            return roleAddedResponse(user, "seller", seller.getId());
        }

        Role role = roleRepository.findByNameAndIsDeletedFalse("ROLE_SELLER")
                .orElseThrow(() -> new AuthenticationException(DochErrorCode.AUTH002));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .userName(request.getUserName())
                .email(request.getEmail())
                .tel(request.getPhone())
                .userRole(role)
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(autoVerifyAccounts)
                .build();
        userRepository.save(user);

        if (autoVerifyAccounts) {
            log.warn("AUTO-VERIFY is ON: seller account {} enabled without email verification (dev/staging only).", user.getEmail());
        }

        Seller seller = Seller.builder()
                .user(user)
                .name(request.getStoreName())
                .city(request.getCity())
                .address(request.getAddress())
                .rating(request.getRating())
                .contactNumber(request.getContactNumber())
                .build();
        sellerRepository.save(seller);

        // Initialize Virtual Wallet
        walletService.getOrCreateWallet(user);

        // Notify Super Admin of new seller registration
        notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT,
                String.format("New seller registered: %s (%s) from %s. Verification pending.",
                        request.getStoreName(), request.getEmail(), request.getCity()));

        if (!autoVerifyAccounts) {
            sendActivationEmailSafely(user);
        }

        return seller.getId();
    }


    @Override
    @Transactional
    public String onboardStaff(StaffOnboardingRequest request, String roleName) {
        var existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            User existing = existingUser.get();
            // A shopper or seller can also become a driver on the same account (same login).
            if (!"ROLE_DELIVERY".equals(roleName) || existing.hasRole("ROLE_DELIVERY")
                    || !COMBINABLE_ROLES.containsAll(existing.getRoleNames())) {
                throw new BusinessException("Staff email already exists.");
            }
            Role driverRole = roleRepository.findByNameAndIsDeletedFalse("ROLE_DELIVERY")
                    .orElseThrow(() -> new BusinessException("Target role not found: ROLE_DELIVERY"));
            existing.getExtraRoles().add(driverRole);
            userRepository.save(existing);
            if (driverProfileRepository.findByUser(existing).isEmpty() && request instanceof DriverOnboardingRequest driverReq) {
                registerDriverVehicle(driverProfileRepository.save(DriverProfile.builder()
                        .user(existing).vehicleType(driverReq.getVehicleType()).plateNumber(driverReq.getPlateNumber()).build()));
            }
            notificationService.sendNotification(existing, NotificationType.SYSTEM_ANNOUNCEMENT,
                    "You have been added to the DOCH delivery team. Sign in to the Driver app with your existing email and password.");
            emailService.sendEmailFromTemplate(existing.getEmail(), "You can now drive for DOCH", "staff-role-added", Map.of(
                    "name", existing.getFirstName() != null ? existing.getFirstName() : "",
                    "role", "Driver",
                    "loginLink", driverBaseUrl + "/login"));
            log.info("ROLE ADDED: existing account {} onboarded as driver", existing.getEmail());
            return "ROLE_DELIVERY added to the existing account " + existing.getEmail()
                    + ". The driver signs in to the Driver app with their existing password.";
        }

        Role role = roleRepository.findByNameAndIsDeletedFalse(roleName)
                .orElseThrow(() -> new BusinessException("Target role not found: " + roleName));

        // Create User with random password (they must set their own during setup)
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .userName(request.getEmail())
                .tel(request.getPhone())
                .userRole(role)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .enabled(false)
                .build();
        userRepository.save(user);

        if (roleName.equals("ROLE_DELIVERY") && request instanceof DriverOnboardingRequest driverReq) {
            registerDriverVehicle(driverProfileRepository.save(DriverProfile.builder()
                    .user(user).vehicleType(driverReq.getVehicleType()).plateNumber(driverReq.getPlateNumber()).build()));
        }

        VerificationToken token = verificationTokenService.createVerificationToken(user, staffInviteExpiryHours * 60L);
        String setupLink = staffSetupLink(user, roleName);
        boolean emailSent = sendStaffInvitationEmail(user, roleName, token, setupLink);

        if (autoVerifyAccounts) {
            // DEV/STAGING MODE: surface the setup link + OTP directly in the API
            // response so onboarding is fully testable without a working email
            // provider. The account still requires /api/auth/staff-setup (password
            // creation) before login — same flow, just without the email hop.
            log.warn("AUTO-VERIFY is ON: returning staff setup link for {} in the API response (dev/staging only).", user.getEmail());
            return roleName + " invited successfully. DEV setup: use OTP " + token.getOtpCode()
                    + " at " + setupLink;
        }

        if (!emailSent) {
            // The account and driver profile exist; only the email failed.
            log.error("Staff onboarding for {} completed, but the invitation email failed to send.", user.getEmail());
            return roleName + " account created, but the invitation email could not be sent. "
                    + "Check the email provider configuration (RESEND_API_KEY / verified sender domain), "
                    + "or enable AUTH_AUTO_VERIFY in dev to receive the setup link directly in this response.";
        }

        return roleName + " invited successfully.";
    }

    @Override
    @Transactional
    public String completeStaffSetup(StaffSetupCompletionRequest request) {
        // 1. Verify OTP
        boolean isValid = verificationTokenService.verifyOtp(request.getOtpCode(), request.getEmail());
        if (!isValid) throw new BusinessException("Invalid or expired code.");

        // 2. Update Password and Enable
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setEnabled(true);
        userRepository.save(user);

        return "Account successfully activated. You can now login. ID : " + user.getId();
    }

    @Override
    @Transactional
    public ResponseEntity<?> login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationException(DochErrorCode.AUTH001); // 401 Unauthorized
        } catch (DisabledException e) {
            boolean staff = userRepository.findByEmail(request.getEmail())
                    .map(u -> u.getUserRole() != null && (u.getUserRole().getName().contains("ADMIN")
                            || "ROLE_DELIVERY".equals(u.getUserRole().getName())))
                    .orElse(false);
            throw new AuthenticationException(staff ? DochErrorCode.AUTH0013 : DochErrorCode.AUTH009);
        } catch (LockedException e) {
            throw new AuthenticationException(DochErrorCode.AUTH007); // 401 Account Locked
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException(DochErrorCode.AUTH002));

        String accessToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.genrateRefreshToken(user);

        saveUserToken(accessToken, refreshToken, user);

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .accessToken(accessToken)
                .userId(user.getId())
                .refreshToken(refreshToken)
                .role(user.getUserRole().getName())
                .roles(user.getRoleNames())
                .message("User login successful")
                .build());
    }

    @Transactional
    public void logout(String accessToken) {
        tokenRepository.findByAccessToken(accessToken).ifPresent(token -> {
            token.setLoggedOut(true);
            tokenRepository.save(token);
            log.info("User logged out of current session.");
        });
    }

    @Override
    @Transactional
    public boolean verifyUser(VerifyUserDTO verifyUserDTO) {
        User user = userRepository.findByEmail(verifyUserDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // SECURITY: Block Staff/Drivers from standard verification to force password setup
        String role = user.getUserRole().getName();
        if (role.contains("ADMIN") || role.equals("ROLE_DELIVERY")) {
            throw new BusinessException("Staff and Drivers must use the setup link provided in their invitation email.");
        }

        boolean isOtpValid = verificationTokenService.verifyOtp(verifyUserDTO.getToken(), verifyUserDTO.getEmail());
        if (isOtpValid) {
            user.setEnabled(true);
            userRepository.save(user);
            // Send in-app verification confirmation
            notificationService.sendNotification(user, NotificationType.ACCOUNT_VERIFIED,
                    "Your account has been verified!");
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account with this email does not exist."));

        VerificationToken token = verificationTokenService.createVerificationToken(user);

        Map<String, Object> context = Map.of(
                "name", user.getFirstName(),
                "otpCode", token.getOtpCode(),
                "expirationMinutes", 15
        );

        emailService.sendEmailFromTemplate(user.getEmail(), "Reset Your DOCH Password", "password-reset", context);
        log.info("Password reset OTP sent to: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Verify OTP
        boolean isOtpValid = verificationTokenService.verifyOtp(request.getOtpCode(), request.getEmail());
        if (!isOtpValid) throw new BusinessException("Invalid or expired reset code.");

        // 2. Update Password
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // The emailed code proves ownership of the address, so a pending invitation (e.g. an expired driver
        // invite) is activated by a password reset as well.
        if (!user.isEnabled()) user.setEnabled(true);
        userRepository.save(user);

        // Confirm password change in-app
        notificationService.sendNotification(user, NotificationType.PASSWORD_RESET,
                "Your password has been reset successfully. If you didn't do this, contact support immediately.");

        // 3. Invalidate all existing sessions for security
        revokeAllTokenByUser(user);
        log.info("SUCCESS: Password reset completed for {}", request.getEmail());
    }

    // --- ACCOUNT MODIFICATION ---

    @Override
    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        // Validate Old Password
        if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPassword())) {
            throw new BusinessException("The current password you entered is incorrect.");
        }

        // Update to New Password
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        // Security: Log out of all other devices
        revokeAllTokenByUser(currentUser);
        log.info("SUCCESS: Password changed by user {}", currentUser.getEmail());
    }

    @Override
    @Transactional
    public void resentVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Cannot resend verification for a non-existent user."));

        if (user.isEnabled()) {
            throw new BusinessException("This account has already been verified.");
        }

        // Create a new token (this implicitly invalidates any old ones)
        VerificationToken token = verificationTokenService.createVerificationToken(user);

        String activationLink = frontendBaseUrl + "/otp-validation/" + user.getEmail();
        long expirationMinutes = java.time.Duration.between(LocalDateTime.now(), token.getExpiresAt()).toMinutes();

        Map<String, Object> emailContext = Map.of(
                "name", user.getFirstName(),
                "otpCode", token.getOtpCode(),
                "expirationMinutes", expirationMinutes,
                "activationLink", activationLink
        );

        emailService.sendEmailFromTemplate(
                user.getEmail(),
                "New DOCH Account Activation Code",
                "account-verification",
                emailContext
        );
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException(DochErrorCode.AUTH001));

        String accessToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.genrateRefreshToken(user);

        // FIX (Xender 403): login used to call revokeAllTokenByUser(user) here,
        // enforcing a single active session per account. With two client apps
        // (DochMarket + DochXender) sharing one user base, logging into the
        // second app silently revoked the first app's token — its next API call
        // (typically the payment step, the last action in a Xender session)
        // then failed with 403. Concurrent sessions are now allowed; tokens are
        // still revoked on logout, password change/reset, and admin suspension.
        saveUserToken(accessToken, refreshToken, user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getUserRole().getName())
                .roles(user.getRoleNames())
                .message("User login was successful")
                .build();
    }

    /**
     * Sends the activation email without letting an email-provider failure roll back
     * the whole registration. If the send fails, the account still exists and the
     * user can request a resend once email is configured.
     */
    private void sendActivationEmailSafely(User user) {
        try {
            sendActivationEmail(user);
        } catch (Exception e) {
            log.error("EMAIL FAILURE: Could not send activation email to {}. "
                    + "Account was created but is unverified. Cause: {}", user.getEmail(), e.getMessage());
        }
    }

    private void sendActivationEmail(User user) {
        VerificationToken token = verificationTokenService.createVerificationToken(user);
        String link = frontendBaseUrl + "/otp-validation/" + user.getEmail() + "?otp=" + token.getOtpCode();

        Map<String, Object> context = Map.of(
                "name", user.getFirstName(),
                "otpCode", token.getOtpCode(),
                "expirationMinutes", 15,
                "activationLink", link
        );
        emailService.sendEmailFromTemplate(user.getEmail(), "Verify Your DOCH Account", "account-verification", context);
    }

    private void revokeAllTokenByUser(User user) {
        List<Token> validTokens = tokenRepository.findAllAccessTokensByUser(user.getId());
        if (!validTokens.isEmpty()) {
            validTokens.forEach(t -> t.setLoggedOut(true));
            tokenRepository.saveAll(validTokens);
        }
    }

    private void saveUserToken(String accessToken, String refreshToken, User user) {
        Token token = new Token();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setLoggedOut(false);
        token.setUser(user);
        tokenRepository.save(token);
    }

    @Transactional
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        String username = jwtUtils.extractUserName(token);

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("No user found"));

        if (jwtUtils.isValidRefreshToken(token, user)) {
            String accessToken = jwtUtils.generateToken(user);
            String refreshTokenValue = jwtUtils.genrateRefreshToken(user);

            // FIX (Xender 403): rotate ONLY the session that presented this
            // refresh token. Revoking all tokens here logged the user out of
            // every other app/device each time one client refreshed.
            tokenRepository.findByRefreshToken(token).ifPresent(t -> {
                t.setLoggedOut(true);
                tokenRepository.save(t);
            });
            saveUserToken(accessToken, refreshTokenValue, user);

            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenValue)
                    .role(user.getUserRole().getName()) // Extract role string
                .roles(user.getRoleNames())
                    .message("User login was successful")
                    .build());
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public ResponseEntity<?> getUserDetails(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found for the provided email."));

        // You can create a UserDTO to return only necessary fields
        UserDetailResponse userDetailResponse = UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userName(user.getUsername())
                .phone(user.getTel())
                .role(user.getUserRole().getName())
                .roles(user.getRoleNames())
                .build();
        return ResponseEntity.ok(userDetailResponse);
    }

}