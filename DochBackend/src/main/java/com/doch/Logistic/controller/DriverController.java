package com.doch.Logistic.controller;

import com.doch.Logistic.dto.DeliveryResponseDTO;
import com.doch.Logistic.dto.LocationDTO;
import com.doch.Logistic.dto.driver.DriverPerformanceDTO;
import com.doch.Logistic.dto.driver.DriverProfileDTO;
import com.doch.Logistic.dto.driver.UpdateAvailabilityDTO;
import com.doch.Logistic.service.DriverService;
import com.doch.User.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "4. Driver Operations", description = "Endpoints for driver workflow and administrative management of the delivery fleet.")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;

    // --- DRIVER WORKFLOW ENDPOINTS ---

    @Operation(
            summary = "Driver: Get my assigned tasks",
            description = "Returns a list of all active delivery tasks (Xender or Market) assigned to the logged-in driver."
    )
    @GetMapping("/me/deliveries")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<List<DeliveryResponseDTO>> getMyAssignedDeliveries(Authentication auth) {
        User driver = (User) auth.getPrincipal();
        return ResponseEntity.ok(driverService.getAssignedDeliveries(driver));
    }

    @Operation(summary = "Driver: My finished jobs (delivered / returned / cancelled)")
    @GetMapping("/me/history")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<List<DeliveryResponseDTO>> getMyHistory(Authentication auth) {
        return ResponseEntity.ok(driverService.getDeliveryHistory((User) auth.getPrincipal()));
    }

    @Operation(summary = "Driver: My profile, assigned vehicle and rating")
    @GetMapping("/me/profile")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DriverProfileDTO> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(driverService.getMyProfile((User) auth.getPrincipal()));
    }

    @Operation(
            summary = "Driver: Update online/offline status",
            description = "Allows a driver to toggle their availability for new tasks."
    )
    @PatchMapping("/me/availability")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<Void> updateMyAvailability(
            @Valid @RequestBody UpdateAvailabilityDTO request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        driverService.updateAvailability(currentUser, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Driver: Update GPS location",
            description = "Mobile app heartbeat. Updates current coordinates for real-time tracking on the admin map."
    )
    @PutMapping("/me/location")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<Void> updateLocation(
            @Valid @RequestBody LocationDTO location,
            Authentication auth) {

        User driver = (User) auth.getPrincipal();
        driverService.updateCurrentLocation(driver, location);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Driver: Get performance stats (Today/Weekly/Monthly)")
    @GetMapping("/me/performance")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DriverPerformanceDTO> getPerformance(Authentication auth) {
        User driver = (User) auth.getPrincipal();
        return ResponseEntity.ok(driverService.getPerformanceStats(driver));
    }

    // --- STAFF / ADMIN OPERATIONS ---

    @Operation(
            summary = "Staff: List ALL registered drivers",
            description = "Super Admin or Xender Admin only. View the master list of all drivers in the system."
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_XENDER', 'MANAGE_USERS', 'ASSIGN_DRIVER')")
    public ResponseEntity<List<DriverProfileDTO>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @Operation(
            summary = "Staff: List only AVAILABLE (Online) drivers",
            description = "Super, Market, or Xender Admins. Used to see who is ready to be assigned to a task."
    )
    @GetMapping("/available")
    @PreAuthorize("hasAnyAuthority('ASSIGN_DRIVER', 'MANAGE_XENDER')")
    public ResponseEntity<List<DriverProfileDTO>> getAvailableDrivers() {
        return ResponseEntity.ok(driverService.getAvailableDrivers());
    }

    @Operation(
            summary = "Staff: Get specific driver profile",
            description = "Super Admin or Xender Admin. Retrieve full details, vehicle info, and ratings for a driver."
    )
    @GetMapping("/{driverId}/profile")
    @PreAuthorize("hasAnyAuthority('VIEW_LOGISTICS_REPORTS', 'MANAGE_XENDER', 'MANAGE_USERS', 'ASSIGN_DRIVER')")
    public ResponseEntity<DriverProfileDTO> getProfile(
            @Parameter(description = "The User UUID of the driver") @PathVariable String driverId) {
        return ResponseEntity.ok(driverService.getDriverProfile(driverId));
    }
}