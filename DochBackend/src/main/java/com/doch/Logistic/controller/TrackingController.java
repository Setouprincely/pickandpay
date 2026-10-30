package com.doch.Logistic.controller;

import com.doch.Logistic.dto.PublicTrackingDTO;
import com.doch.Logistic.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Tag(name = "12. Public Tracking", description = "Anonymous package tracking by tracking number (no personal data).")
public class TrackingController {

    private final DeliveryService deliveryService;

    @Operation(summary = "Track a package", description = "Returns the current status and the public tracking history. No names, addresses, phone numbers or codes.")
    @GetMapping("/{trackingNumber}")
    public ResponseEntity<PublicTrackingDTO> track(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(deliveryService.publicTracking(trackingNumber));
    }
}