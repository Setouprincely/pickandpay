package com.doch.Payment.repository;

import com.doch.Payment.model.Wallet;
import com.doch.User.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    Optional<Wallet> findByUserId(String userId);

    // Used by Admin to see everyone who is owed money
    List<Wallet> findAllByAvailableBalanceGreaterThan(Double amount);
}