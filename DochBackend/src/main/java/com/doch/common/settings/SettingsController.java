package com.doch.common.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "10. Platform Settings", description = "Super Admin endpoints for platform-level configuration such as delivery pricing.")
@SecurityRequirement(name = "bearerAuth")
public class SettingsController {

    private final PricingSettingsService pricingSettingsService;

    @Operation(summary = "Admin: Get current pricing configuration",
            description = "Returns the delivery pricing configuration: base price, price per km, minimum price, and peak surcharge.")
    @GetMapping("/pricing")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_XENDER')")
    public ResponseEntity<PricingSettingsDTO> getPricingSettings() {
        return ResponseEntity.ok(pricingSettingsService.getSettings());
    }

    @Operation(summary = "Admin: Update pricing configuration",
            description = "Partial update: only non-null fields are applied. Changes take effect immediately: the peak surcharge and minimum price floor are applied by the pricing engine on the next quote.")
    @PutMapping("/pricing")
    @PreAuthorize("hasAuthority('MANAGE_SYSTEM')")
    public ResponseEntity<PricingSettingsDTO> updatePricingSettings(@Valid @RequestBody PricingSettingsDTO request) {
        return ResponseEntity.ok(pricingSettingsService.updateSettings(request));
    }
}
