package com.doch.Payment.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WalletResponseDTO {
    private String walletId;
    private Double pendingBalance;   // Money in escrow (delivery in progress)
    private Double availableBalance; // Money ready for the 1st/16th payout
    private Double totalWithdrawn;  // Total real money received so far
}