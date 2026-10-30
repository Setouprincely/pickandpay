package com.doch.Fleet.controller;

import com.doch.Fleet.dto.VehicleRequestDTO;
import com.doch.Fleet.dto.VehicleResponseDTO;
import com.doch.Fleet.service.FleetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
@Tag(name = "8. Fleet Management", description = "Admin endpoints for managing the vehicle fleet: add/edit/delete vehicles, assign drivers, and track status and maintenance.")
@SecurityRequirement(name = "bearerAuth")
public class FleetController {

    private final FleetService fleetService;

    @Operation(summary = "Admin: List all vehicles", description = "Optionally filter by status (ACTIVE, IN_MAINTENANCE, INACTIVE, RETIRED).")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER', 'MANAGE_MARKET')")
    public ResponseEntity<List<VehicleResponseDTO>> getAllVehicles(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) String status) {
        return ResponseEntity.ok(fleetService.getAllVehicles(status));
    }

    @Operation(summary = "Admin: Get a vehicle by ID")
    @GetMapping("/{vehicleId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER', 'MANAGE_MARKET')")
    public ResponseEntity<VehicleResponseDTO> getVehicle(@PathVariable String vehicleId) {
        return ResponseEntity.ok(fleetService.getVehicle(vehicleId));
    }

    @Operation(summary = "Admin: Add a new vehicle to the fleet")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER')")
    public ResponseEntity<VehicleResponseDTO> createVehicle(@Valid @RequestBody VehicleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fleetService.createVehicle(request));
    }

    @Operation(summary = "Admin: Update a vehicle", description = "Partial update: only non-null fields are applied.")
    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER')")
    public ResponseEntity<VehicleResponseDTO> updateVehicle(
            @PathVariable String vehicleId,
            @RequestBody VehicleRequestDTO request) {
        return ResponseEntity.ok(fleetService.updateVehicle(vehicleId, request));
    }

    @Operation(summary = "Admin: Delete a vehicle", description = "Soft delete. The vehicle is removed from all listings.")
    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable String vehicleId) {
        fleetService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Admin: Assign a driver to a vehicle", description = "Pass the DriverProfile ID (not the User ID). A driver can only hold one vehicle at a time.")
    @PatchMapping("/{vehicleId}/assign")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER', 'ASSIGN_DRIVER')")
    public ResponseEntity<VehicleResponseDTO> assignDriver(
            @PathVariable String vehicleId,
            @RequestParam String driverProfileId) {
        return ResponseEntity.ok(fleetService.assignDriver(vehicleId, driverProfileId));
    }

    @Operation(summary = "Admin: Unassign the current driver from a vehicle")
    @PatchMapping("/{vehicleId}/unassign")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER', 'ASSIGN_DRIVER')")
    public ResponseEntity<VehicleResponseDTO> unassignDriver(@PathVariable String vehicleId) {
        return ResponseEntity.ok(fleetService.unassignDriver(vehicleId));
    }

    @Operation(summary = "Admin: Update vehicle status", description = "ACTIVE, IN_MAINTENANCE, INACTIVE, or RETIRED.")
    @PatchMapping("/{vehicleId}/status")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER')")
    public ResponseEntity<VehicleResponseDTO> updateStatus(
            @PathVariable String vehicleId,
            @RequestParam String status) {
        return ResponseEntity.ok(fleetService.updateStatus(vehicleId, status));
    }
}
