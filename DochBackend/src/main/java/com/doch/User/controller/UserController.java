package com.doch.User.controller;

import com.doch.User.dto.Request.ChangePasswordRequest;
import com.doch.User.dto.UserProfileResponse;
import com.doch.User.dto.Request.UpdateProfileRequest;
import com.doch.User.service.interfaces.AuthService;
import com.doch.User.service.interfaces.UserService;
import com.doch.User.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "8. User Profiles", description = "Endpoints for users to manage their identities, update role-specific information, and view public seller stores.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(
            summary = "Get my profile",
            description = "Returns full details of the logged-in user, including role-specific data (Seller stats or Driver vehicle info) if applicable."
    )
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        return ResponseEntity.ok(userService.getUserProfile(currentUser.getId()));
    }

    @Operation(
            summary = "Update my profile info",
            description = "Allows updating base info (Name, Phone) and role-specific info (Store details for Sellers, Vehicle details for Drivers). " +
                    "The system will reject role-specific data if you do not have the corresponding profile type."
    )
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        return ResponseEntity.ok(userService.updateUserProfile(currentUser.getId(), request));
    }

    @Operation(
            summary = "Update my password",
            description = "Requires current password verification. Success will invalidate all other active sessions (force logout on other devices)."
    )
    @PutMapping("/me/password")
    public ResponseEntity<String> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        authService.changePassword(currentUser, request);
        return ResponseEntity.ok("Password updated successfully.");
    }

    @Operation(
            summary = "View a Seller's Public Store",
            description = "Publicly accessible endpoint to view store details and rating for a specific seller."
    )
    @GetMapping("/sellers/{sellerId}")
    @PreAuthorize("permitAll()") // Ensure anyone can view a shop
    public ResponseEntity<UserProfileResponse> getSellerStore(@PathVariable String sellerId) {
        return ResponseEntity.ok(userService.getUserProfile(sellerId));
    }
}