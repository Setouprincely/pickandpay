package com.doch.Payment.service.impl;

import com.doch.Payment.dto.WalletTransactionDTO;
import com.doch.Payment.model.Wallet;
import com.doch.Payment.model.WalletTransaction;
import com.doch.Payment.model.WalletTransaction.TransactionType;
import com.doch.Payment.repository.WalletRepository;
import com.doch.Payment.repository.WalletTransactionRepository;
import com.doch.Payment.service.WalletService;
import com.doch.User.model.User;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Override
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("Creating new virtual wallet for user: {}", user.getEmail());
                    return walletRepository.save(Wallet.builder()
                            .user(user)
                            .pendingBalance(0.0)
                            .availableBalance(0.0)
                            .totalWithdrawn(0.0)
                            .build());
                });
    }

    @Override
    @Transactional
    public void creditPending(User seller, Double amount, String orderId) {
        Wallet wallet = getOrCreateWallet(seller);

        wallet.setPendingBalance(wallet.getPendingBalance() + amount);
        walletRepository.save(wallet);

        createTransaction(wallet, amount, TransactionType.CREDIT_PENDING,
                "Pending earnings from Marketplace Order #" + orderId);

        log.info("WALLET: Credited {} PENDING to seller {}", amount, seller.getEmail());
    }

    @Override
    @Transactional
    public void releaseToAvailable(User seller, Double amount, String orderId) {
        Wallet wallet = walletRepository.findByUserId(seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for seller"));

        if (wallet.getPendingBalance() < amount) {
            log.warn("Wallet balance mismatch for seller {}. Attempting to release {}, but only {} pending.",
                    seller.getEmail(), amount, wallet.getPendingBalance());
        }

        // Logic: Move from Pending to Available
        wallet.setPendingBalance(Math.max(0, wallet.getPendingBalance() - amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance() + amount);
        walletRepository.save(wallet);

        createTransaction(wallet, amount, TransactionType.RELEASE_TO_AVAILABLE,
                "Funds released for completed Order #" + orderId);

        log.info("WALLET: Released {} to AVAILABLE for seller {}", amount, seller.getEmail());
    }

    @Override
    @Transactional
    public void reversePending(User seller, Double amount, String orderId) {
        Wallet wallet = getOrCreateWallet(seller);
        wallet.setPendingBalance(Math.max(0, wallet.getPendingBalance() - amount));
        walletRepository.save(wallet);
        createTransaction(wallet, amount, TransactionType.REVERSE_PENDING,
                "Pending earnings reversed for cancelled/returned Order #" + orderId);
        log.info("WALLET: Reversed {} PENDING for seller {}", amount, seller.getEmail());
    }
    @Override
    @Transactional
    public void processManualPayout(String walletId, Double amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getAvailableBalance() < amount) {
            throw new BusinessException("Insufficient available balance for this payout.");
        }

        // Logic: Debit Available and move to Total Withdrawn
        wallet.setAvailableBalance(wallet.getAvailableBalance() - amount);
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn() + amount);
        walletRepository.save(wallet);

        createTransaction(wallet, amount, TransactionType.PAYOUT_DEBIT,
                "Manual payout processed by Administrator");

        log.info("WALLET: Payout of {} recorded for wallet {}", amount, walletId);
    }

    @Override
    public Wallet getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionDTO> getTransactionHistory(String userId) {
        // If the wallet doesn't exist, it means there are no transactions.
        // We return an empty list instead of throwing an exception.
        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);

        if (walletOpt.isEmpty()) {
            return Collections.emptyList();
        }

        return transactionRepository.findByWalletOrderByCreatedAtDesc(walletOpt.get()).stream()
                .map(tx -> WalletTransactionDTO.builder()
                        .id(tx.getId())
                        .amount(tx.getAmount())
                        .type(tx.getType().name())
                        .description(tx.getDescription())
                        .date(tx.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                        .build())
                .collect(Collectors.toList());
    }

    // --- Private Helper to record the Ledger ---
    private void createTransaction(Wallet wallet, Double amount, TransactionType type, String desc) {
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .description(desc)
                .build();
        transactionRepository.save(tx);
    }
}