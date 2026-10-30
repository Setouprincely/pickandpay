package com.doch.User.controller;

import com.doch.User.dto.Request.*;
import com.doch.User.service.interfaces.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Endpoints")
public class AuthenticationController {
    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Authenticate User",
            description = "Authenticate a user with email and password to receive a JWT token.",
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        System.out.println("Authentication controller === " + authService);
        return authService.login(authRequest);
    }

    @Operation(
            summary = "Logout user (Current Session)",
            description = "Invalidates the current Bearer token by marking it as 'logged out' in the database. " +
                    "This ensures the specific token cannot be used again. Other active sessions on different " +
                    "devices will remain active."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully logged out. No content returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Token is missing, expired, or already logged out.")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Register New User",
            description = "Register a new user with the provided details.",
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid UserRegistration user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Collect validation errors and return a 400 Bad Request response
            List<String> errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }
        return ResponseEntity.ok(authService.createAccount(user));
    }

    @Operation(
            summary = "Register New Seller Account",
            description = "Register a new seller with the provided details.",
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Seller registered successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = "application/json"))
    })

    @PostMapping("/seller/register")
    public ResponseEntity<?> registerSeller(@RequestBody @Valid SellerRegistration user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Collect validation errors and return a 400 Bad Request response
            List<String> errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }
        return ResponseEntity.ok(authService.createSellerAccount(user));
    }

    @Operation(
            summary = "Verify User",
            description = "Verify a user using their email and verification code.",
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification successful",
                    content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "400", description = "Verification failed",
                    content = @Content(mediaType = "text/plain"))
    })

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<String> verifyUser(@RequestBody VerifyUserDTO verifyUserDTO) {
        boolean verified = authService.verifyUser(verifyUserDTO);
        if (verified) {
            return new ResponseEntity<>("User verified", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Verification failed", HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Verify User",
            description = "Verify a user using their email and verification code.",
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification successful",
                    content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "400", description = "Verification failed",
                    content = @Content(mediaType = "text/plain"))
    })

    @GetMapping("/resend")
    @Transactional
    public ResponseEntity<Void> resentOTP(@RequestParam String email) {
        authService.resentVerificationEmail(email);
        return ResponseEntity.status(NO_CONTENT).build();
    }

//     @Operation(
//             summary = "Get User Details",
//             description = "Retrieve details of a user by their email.",
//             tags = {"Authentication"}
//     )
//     @ApiResponses(value = {
//             @ApiResponse(responseCode = "200", description = "User details retrieved successfully",
//                     content = @Content(mediaType = "application/json")),
//             @ApiResponse(responseCode = "400", description = "User details not found",
//                     content = @Content(mediaType = "text/plain"))
//     })

//     @GetMapping("/details")
//     @Transactional
//     public ResponseEntity<?> getUserDetails(@RequestParam String email) {
//         return ResponseEntity.ok(authService.getUserDetails(email));
//     }

    @Operation(
            summary = "Complete Staff Setup (Admin/Driver)",
            description = "For invited staff members (Admins or Drivers) to set their permanent password and activate their account using the OTP received via email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff account activated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP.")
    })
    @PostMapping("/staff-setup")
    public ResponseEntity<String> completeStaffSetup(@Valid @RequestBody StaffSetupCompletionRequest request) {
        // Updated to use the generic completeStaffSetup method
        return ResponseEntity.ok(authService.completeStaffSetup(request));
    }

    @Operation(summary = "Send a new staff/driver invitation code",
            description = "For invited staff and drivers whose code expired. Always answers the same way so it cannot be used to probe accounts.",
            tags = {"Authentication"})
    @PostMapping("/staff-setup/resend")
    public ResponseEntity<Map<String, String>> resendStaffInvitation(@RequestParam String email) {
        authService.resendStaffInvitation(email);
        return ResponseEntity.ok(Map.of("message",
                "If this email has a pending DOCH invitation, a new code has been sent to it."));
    }

    @Operation(summary = "Forgot Password: Send OTP", description = "Sends a 6-digit reset code to the user's email.")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok("Reset code sent to your email.");
    }

    @Operation(summary = "Forgot Password: Reset using OTP", description = "Verifies the OTP and sets the new permanent password.")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully. You can now login.");
    }
}
