package com.doch.Payment.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletTransactionDTO {
    private String id;
    private Double amount;
    private String type; // CREDIT_PENDING, RELEASE_TO_AVAILABLE, PAYOUT_DEBIT
    private String description;
    private LocalDateTime date;
}