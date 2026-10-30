package com.doch.Support.controller;

import com.doch.Support.dto.SupportReportRequestDTO;
import com.doch.Support.dto.SupportReportResponseDTO;
import com.doch.Support.service.SupportReportService;
import com.doch.User.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "9. Support & Problem Reports", description = "Endpoints for drivers (and other users) to report problems, and for admins to review them.")
@SecurityRequirement(name = "bearerAuth")
public class SupportController {

    private final SupportReportService supportReportService;

    @Operation(summary = "Submit a problem report",
            description = "Used primarily by the Driver App 'Report Problem' feature. Any authenticated user can submit. Categories: VEHICLE_ISSUE, CUSTOMER_ISSUE, APP_BUG, PAYMENT_ISSUE, OTHER.")
    @PostMapping("/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportReportResponseDTO> submitReport(
            @Valid @RequestBody SupportReportRequestDTO request,
            Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(supportReportService.submitReport(currentUser, request));
    }

    @Operation(summary = "Get my submitted reports", description = "Returns the logged-in user's own problem reports, newest first.")
    @GetMapping("/reports/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SupportReportResponseDTO>> getMyReports(Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        return ResponseEntity.ok(supportReportService.getMyReports(currentUser));
    }

    @Operation(summary = "Admin: List all problem reports", description = "Optionally filter by status: OPEN, IN_REVIEW, RESOLVED, DISMISSED.")
    @GetMapping("/reports")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER', 'MANAGE_MARKET')")
    public ResponseEntity<List<SupportReportResponseDTO>> getAllReports(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) String status) {
        return ResponseEntity.ok(supportReportService.getAllReports(status));
    }

    @Operation(summary = "Admin: Update a report's status", description = "Move a report to IN_REVIEW, RESOLVED, or DISMISSED, optionally attaching a resolution note.")
    @PatchMapping("/reports/{reportId}/status")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER', 'MANAGE_MARKET')")
    public ResponseEntity<SupportReportResponseDTO> updateStatus(
            @PathVariable String reportId,
            @RequestParam String status,
            @RequestParam(required = false) String resolutionNote) {
        return ResponseEntity.ok(supportReportService.updateStatus(reportId, status, resolutionNote));
    }
}
