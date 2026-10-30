package com.doch.Payment.repository;

import com.doch.Payment.model.Wallet;
import com.doch.Payment.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
}