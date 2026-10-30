package com.doch.Payment.service;

import com.doch.Payment.dto.WalletTransactionDTO;
import com.doch.Payment.model.Wallet;
import com.doch.User.model.User;

import java.util.List;

public interface WalletService {

    // Called when a seller is created or during first sale
    Wallet getOrCreateWallet(User user);

    // Called when buyer pays: Money goes to PENDING
    void creditPending(User seller, Double amount, String orderId);

    // Called when delivery is confirmed: Money moves from PENDING to AVAILABLE
    void releaseToAvailable(User seller, Double amount, String orderId);

    /** Withdraws pending (not yet released) earnings when an order leg is cancelled or returned. */
    void reversePending(User seller, Double amount, String orderId);

    // Called by Admin when they manually send Momo/Cash to the seller
    void processManualPayout(String walletId, Double amount);

    List<WalletTransactionDTO> getTransactionHistory(String userId);

    // Fetch balance for the Seller Portal
    Wallet getWalletByUserId(String userId);
}