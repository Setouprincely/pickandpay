package com.doch.Payment.controller;

import com.doch.Payment.dto.WalletResponseDTO;
import com.doch.Payment.dto.WalletTransactionDTO;
import com.doch.Payment.model.Wallet;
import com.doch.Payment.service.WalletService;
import com.doch.User.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "7. Personal Wallet", description = "Endpoints for users (Sellers/Buyers) to view their virtual balances and ledger history.")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @Operation(
            summary = "Get my wallet balance",
            description = "Returns pending, available, and withdrawn balances. If no wallet exists for the user, one is created automatically with zero balance."
    )
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('VIEW_OWN_FINANCE')") // Permission-based security
    public ResponseEntity<WalletResponseDTO> getMyWallet(Authentication auth) {
        User currentUser = (User) auth.getPrincipal();

        // Use getOrCreate to ensure new users don't get a 404
        Wallet wallet = walletService.getOrCreateWallet(currentUser);

        return ResponseEntity.ok(WalletResponseDTO.builder()
                .walletId(wallet.getId())
                .pendingBalance(wallet.getPendingBalance())
                .availableBalance(wallet.getAvailableBalance())
                .totalWithdrawn(wallet.getTotalWithdrawn())
                .build());
    }

    @Operation(
            summary = "Get my transaction history",
            description = "Returns a chronological list of all virtual money movements (credits, releases, and payouts)."
    )
    @GetMapping("/me/history")
    @PreAuthorize("hasAuthority('VIEW_OWN_FINANCE')") // Permission-based security
    public ResponseEntity<List<WalletTransactionDTO>> getMyHistory(Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        return ResponseEntity.ok(walletService.getTransactionHistory(currentUser.getId()));
    }
}