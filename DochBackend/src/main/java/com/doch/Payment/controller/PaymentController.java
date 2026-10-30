package com.doch.Payment.controller;

import com.doch.Payment.dto.PaymentRequestDTO;
import com.doch.Payment.dto.PaymentResponseDTO;
import com.doch.Payment.service.PaymentService;
import com.doch.User.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Endpoints", description = "Endpoints for initiating, retrying and verifying payments.")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Start or retry a payment",
            description = """
                    Pays for an order (orderId) or a DochXender request (xenderRequestId) owned by the caller.
                    The amount is always taken from the server-side record.
                    - Without `phone`: returns a Fapshi hosted payment link (`paymentUrl`).
                    - With `phone` (e.g. 670000000): sends a mobile money prompt to that phone (direct pay).
                    A failed or expired attempt can be retried by calling this endpoint again.""")
    @ApiResponse(responseCode = "200", description = "Payment attempt created (check `status`: PENDING or FAILED).")
    @ApiResponse(responseCode = "403", description = "The order / request belongs to another user.")
    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<PaymentResponseDTO> initiatePayment(@Valid @RequestBody PaymentRequestDTO request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.initiatePayment(request, currentUser));
    }

    @Operation(summary = "Check payment status",
            description = "Returns the payment status. Pending payments are re-checked with the provider. Owner or payment staff only.")
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(
            @Parameter(description = "The internal ID of the payment to check.") @PathVariable String paymentId,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.checkPaymentStatus(paymentId, currentUser));
    }

    @Operation(summary = "Admin: Reconcile a payment with the provider",
            description = "Forces a provider status check and applies the result (idempotent).")
    @PostMapping("/{paymentId}/reconcile")
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_MARKET', 'MANAGE_XENDER')")
    public ResponseEntity<PaymentResponseDTO> reconcile(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.reconcile(paymentId));
    }
}