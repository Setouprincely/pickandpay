package com.doch.User.controller;

import com.doch.User.dto.Request.DriverOnboardingRequest;
import com.doch.User.dto.Request.StaffOnboardingRequest;
import com.doch.User.dto.UserProfileResponse;
import com.doch.User.service.interfaces.AuthService;
import com.doch.User.service.interfaces.UserService;
import com.doch.common.exceptions.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "1. Admin & Staff Management", description = "Endpoints for Super Admins to onboard specialized staff and manage user profiles.")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Unified Onboarding for specialized Admins.
     * Only the Super Admin (who has MANAGE_USERS permission) can call this.
     */
    @Operation(
            summary = "Onboard a specialized Admin (Market or Xender)",
            description = "Super Admin Only. Creates a specialized admin account and sends an invitation email."
    )
    @PostMapping("/onboard-staff")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<String> onboardSpecializedStaff(
            @Valid @RequestBody StaffOnboardingRequest request,
            @Parameter(description = "The specific role: ROLE_MARKET_ADMIN or ROLE_XENDER_ADMIN")
            @RequestParam String roleName) {

        // Validate that we are only creating specific Admin roles here
        List<String> allowedRoles = Arrays.asList("ROLE_MARKET_ADMIN", "ROLE_XENDER_ADMIN");
        if (!allowedRoles.contains(roleName.toUpperCase())) {
            throw new BusinessException("Invalid role. Use /onboard-driver for drivers or check role name.");
        }

        return ResponseEntity.ok(authService.onboardStaff(request, roleName.toUpperCase()));
    }

    @Operation(
            summary = "Onboard a new Driver",
            description = "Super Admin Only. Creates a driver account with vehicle details and sends an invitation."
    )
    @PostMapping("/onboard-driver")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<String> onboardDriver(@Valid @RequestBody DriverOnboardingRequest request) {
        return ResponseEntity.ok(authService.onboardStaff(request, "ROLE_DELIVERY"));
    }

    /**
     * General User Management.
     * Accessible by anyone with MANAGE_USERS or MANAGE_SYSTEM (Super, Market, or Xender Admins).
     */
    @Operation(summary = "Get any user profile details", description = "Admin view of full user profile (Driver/Seller stats included).")
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_SYSTEM')")
    public ResponseEntity<UserProfileResponse> adminGetUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @Operation(summary = "List all registered users", description = "Admin view of all platform users.")
    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_SYSTEM')")
    public ResponseEntity<List<UserProfileResponse>> adminListUsers() {
        return ResponseEntity.ok(userService.getAllUserProfiles());
    }

    @Operation(
            summary = "Onboard a new Super Admin",
            description = "Super Admin Only. Use with extreme caution. This grants absolute power over the system."
    )
    @PostMapping("/onboard-super-admin")
    @PreAuthorize("hasAuthority('MANAGE_SYSTEM')")
    public ResponseEntity<String> onboardSuperAdmin(@Valid @RequestBody StaffOnboardingRequest request) {
        return ResponseEntity.ok(authService.onboardStaff(request, "ROLE_SUPER_ADMIN"));
    }

    @Operation(
            summary = "Change any user's role",
            description = "Super Admin Only. Allows changing a user's role (e.g., Buyer to Seller, or Seller to Market Admin)."
    )
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAuthority('MANAGE_SYSTEM')")
    public ResponseEntity<String> updateUserRole(
            @PathVariable String userId,
            @Parameter(description = "Target role name, e.g., ROLE_SELLER, ROLE_SUPER_ADMIN")
            @RequestParam String newRoleName) {

        userService.updateUserRole(userId, newRoleName.toUpperCase());
        return ResponseEntity.ok("User role updated to " + newRoleName + " successfully.");
    }

    @Operation(
            summary = "Suspend or reactivate a user account",
            description = "Admin only. Pass enabled=false to suspend (the user is logged out of all sessions and can no longer log in) or enabled=true to reactivate. Admins cannot suspend themselves or the bootstrap super admin."
    )
    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_SYSTEM')")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable String userId,
            @Parameter(description = "true = active, false = suspended")
            @RequestParam boolean enabled,
            org.springframework.security.core.Authentication auth) {

        com.doch.User.model.User actingAdmin = (com.doch.User.model.User) auth.getPrincipal();
        userService.updateUserStatus(userId, enabled, actingAdmin);
        return ResponseEntity.ok("User account " + (enabled ? "reactivated" : "suspended") + " successfully.");
    }

    @Operation(summary = "Remove one role (user type) from an account",
            description = "Takes buyer, seller or driver access away while the person keeps their other roles. "
                    + "Removing seller access un-verifies the store (its products disappear from the marketplace); "
                    + "removing driver access is refused while the driver carries active packages.")
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserProfileResponse> removeUserRole(@PathVariable String userId, @PathVariable String roleName,
                                                              org.springframework.security.core.Authentication auth) {
        com.doch.User.model.User actingAdmin = (com.doch.User.model.User) auth.getPrincipal();
        return ResponseEntity.ok(userService.removeUserRole(userId, roleName.toUpperCase(), actingAdmin));
    }

    @Operation(summary = "Delete a user account",
            description = "Soft delete: the account can no longer sign in and disappears from lists, while its orders, "
                    + "payments and audit history are kept. The email can be registered again.")
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<java.util.Map<String, String>> deleteUser(@PathVariable String userId,
                                                                    org.springframework.security.core.Authentication auth) {
        com.doch.User.model.User actingAdmin = (com.doch.User.model.User) auth.getPrincipal();
        userService.deleteUser(userId, actingAdmin);
        return ResponseEntity.ok(java.util.Map.of("message", "User deleted."));
    }
}
