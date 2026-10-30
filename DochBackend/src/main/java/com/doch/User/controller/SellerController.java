package com.doch.User.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.doch.User.dto.SellerResponse;
import com.doch.User.dto.Request.SellerRequest;
import com.doch.User.model.User;
import com.doch.User.service.interfaces.SellerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "7. Seller Management", description = "Endpoints for seller profiles: self-service creation, admin verification, and lookups.")
public class SellerController {

    private final SellerService sellerService;

    @Operation(
            summary = "Create a seller profile",
            description = """
                    Requires authentication (a valid Bearer token).
                    - Omit userId (or pass your own) to create a seller profile for YOUR account. Any authenticated user can do this.
                    - Passing a different userId requires admin authority (MANAGE_USERS / MANAGE_SYSTEM / MANAGE_MARKET).
                    Returns the new seller profile ID. Fails if the user already has a profile.""",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Seller"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Seller profile created; body is the new seller ID",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "User already has a profile, or non-admin tried to create for another user",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> createSeller(@RequestBody SellerRequest request, Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerService.createSeller(request, currentUser));
    }

    @Operation(summary = "Admin: List all sellers with verification status")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all")
    public List<SellerResponse> getAllSellers() {
        return sellerService.getAllSellers();
    }

    @Operation(summary = "Get seller profiles by user ID")
    @GetMapping("/byUserId")
    public List<SellerResponse> getSellerByUserId(@RequestParam String userId) {
        return sellerService.getSellerByUserId(userId);
    }

    @Operation(summary = "Get a seller by seller ID")
    @GetMapping("/{sellerId}")
    public SellerResponse getSellerById(@PathVariable String sellerId) {
        return sellerService.getSellerById(sellerId);
    }

    @Operation(summary = "List sellers by city")
    @GetMapping("/byCity")
    public List<SellerResponse> getSellersByCity(@RequestParam String city) {
        return sellerService.getSellersByCity(city);
    }

    @Operation(
            summary = "Admin: Delete a seller profile (soft delete)",
            description = "Delete an existing seller. Requires MANAGE_MARKET or MANAGE_SYSTEM.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Seller"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires MANAGE_MARKET or MANAGE_SYSTEM",
                    content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/delete/{sellerId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    @SecurityRequirement(name = "bearerAuth")
    public void deleteSeller(@PathVariable String sellerId) {
        sellerService.deleteSeller(sellerId);
    }

    @Operation(
            summary = "Admin: Verify or un-verify a seller (Vendor Agreement)",
            description = "Update the verification status of an existing seller. Requires MANAGE_MARKET or MANAGE_SYSTEM.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Seller"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller verification updated successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires MANAGE_MARKET or MANAGE_SYSTEM",
                    content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/verify/{sellerId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    @SecurityRequirement(name = "bearerAuth")
    public SellerResponse updateSellerVerification(@PathVariable String sellerId, @RequestParam Boolean isVerified,
                                                   @RequestParam(required = false) String note) {
        return sellerService.updateSellerVerification(sellerId, isVerified, note);
    }
}