package com.doch.Payment.controller;

import com.doch.Payment.model.Wallet;
import com.doch.Payment.repository.WalletRepository;
import com.doch.Payment.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payouts")
@RequiredArgsConstructor
@Tag(name = "6. Payout Management (Admin)", description = "Endpoints for managing bi-monthly seller payouts. Restricted to Super Admins.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('MANAGE_SYSTEM')") // Restricted to Super Admin for financial integrity
public class PayoutAdminController {

    private final WalletRepository walletRepository;
    private final WalletService walletService;

    @Operation(
            summary = "Super Admin: List all sellers eligible for payout",
            description = "Returns all virtual wallets that have an 'availableBalance' greater than zero. Perform manual Momo/Bank transfers based on this list on the 1st and 16th."
    )
    @GetMapping("/eligible")
    public ResponseEntity<List<Wallet>> getEligiblePayouts() {
        return ResponseEntity.ok(walletRepository.findAllByAvailableBalanceGreaterThan(0.0));
    }

    @Operation(
            summary = "Super Admin: Mark a manual payout as completed",
            description = "After manually sending money to the seller, call this to update the virtual ledger. Moves funds from 'Available' to 'Total Withdrawn'."
    )
    @PostMapping("/complete/{walletId}")
    public ResponseEntity<String> markPayoutAsCompleted(
            @Parameter(description = "The UUID of the seller's wallet") @PathVariable String walletId,
            @Parameter(description = "The exact amount of money sent to the seller") @RequestParam Double amount) {

        walletService.processManualPayout(walletId, amount);
        return ResponseEntity.ok("Payout successfully recorded. Virtual balance updated.");
    }

    @Operation(
            summary = "Super Admin: View specific wallet details",
            description = "Retrieve full balance details and history for a specific wallet."
    )
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<Wallet> getWalletDetails(@PathVariable String walletId) {
        return ResponseEntity.ok(walletRepository.findById(walletId)
                .orElseThrow(() -> new com.doch.common.exceptions.ResourceNotFoundException("Wallet not found")));
    }
}