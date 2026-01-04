package com.exchange.exchange.service;

import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.entity.key.WalletId;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private com.exchange.exchange.repository.WalletTransactionRepository transactionRepository;

    // 取得某會員所有錢包
    public List<Wallet> getWallets(Integer memberId) {
        return walletRepository.findByMemberId(memberId);
    }

    // 取得資金流水
    public List<com.exchange.exchange.entity.WalletTransaction> getTransactions(Integer memberId) {
        return transactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    // 取得單一錢包 (若不存在則創建空錢包)
    public Wallet getWallet(Integer memberId, String coinId) {
        String cleanCoinId = coinId != null ? coinId.trim() : null;
        return walletRepository.findById(new WalletId(memberId, cleanCoinId))
                .orElseGet(() -> createEmptyWallet(memberId, cleanCoinId));
    }

    // 模擬儲值
    @Transactional
    public Wallet deposit(Integer memberId, String coinId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        String cleanCoinId = coinId != null ? coinId.trim() : null;
        if (cleanCoinId == null || cleanCoinId.isEmpty()) {
            throw new IllegalArgumentException("Coin ID cannot be empty");
        }

        // 驗證 Coin 是否存在
        if (!coinRepository.existsById(cleanCoinId)) {
            throw new IllegalArgumentException("Coin not found: " + cleanCoinId);
        }

        Wallet wallet = getWallet(memberId, cleanCoinId);
        
        // 增加總餘額與可用餘額
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setAvailable(wallet.getAvailable().add(amount));
        
        // Log transaction
        com.exchange.exchange.entity.WalletTransaction tx = new com.exchange.exchange.entity.WalletTransaction(
            memberId, cleanCoinId, "DEPOSIT", amount
        );
        transactionRepository.save(tx);

        return walletRepository.save(wallet);
    }

    // 結算盈虧 (可正可負)
    @Transactional
    public void realizePnL(Integer memberId, String coinId, BigDecimal pnlAmount) {
        if (pnlAmount.compareTo(BigDecimal.ZERO) == 0) return;

        Wallet wallet = getWallet(memberId, coinId);
        
        // Update Balance & Available
        wallet.setBalance(wallet.getBalance().add(pnlAmount));
        wallet.setAvailable(wallet.getAvailable().add(pnlAmount));
        
        walletRepository.save(wallet);

        // Log Transaction
        com.exchange.exchange.entity.WalletTransaction tx = new com.exchange.exchange.entity.WalletTransaction(
            memberId, coinId, "REALIZED_PNL", pnlAmount
        );
        transactionRepository.save(tx);
    }

    // Deduct Frozen Funds (Spent)
    @Transactional
    public void deductFrozen(Integer memberId, String coinId, BigDecimal amount, String type) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        
        Wallet wallet = getWallet(memberId, coinId);
        // Balance decreases, Available stays same (already deducted on freeze)
        wallet.setBalance(wallet.getBalance().subtract(amount));
        
        walletRepository.save(wallet);
        
        transactionRepository.save(new com.exchange.exchange.entity.WalletTransaction(
            memberId, coinId, type, amount.negate()
        ));
    }

    // Add Balance (Proceeds)
    @Transactional
    public void addBalance(Integer memberId, String coinId, BigDecimal amount, String type) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        Wallet wallet = getWallet(memberId, coinId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setAvailable(wallet.getAvailable().add(amount));

        walletRepository.save(wallet);

        transactionRepository.save(new com.exchange.exchange.entity.WalletTransaction(
            memberId, coinId, type, amount
        ));
    }

    // 重置所有錢包
    @Transactional
    public void resetWallets(Integer memberId) {
        List<Wallet> wallets = walletRepository.findByMemberId(memberId);
        for (Wallet wallet : wallets) {
            BigDecimal oldBalance = wallet.getBalance();
            if (oldBalance.compareTo(BigDecimal.ZERO) > 0) {
                // Log RESET (Withdraw all)
                 com.exchange.exchange.entity.WalletTransaction tx = new com.exchange.exchange.entity.WalletTransaction(
                    memberId, wallet.getCoinId(), "WITHDRAW (RESET)", oldBalance.negate()
                );
                transactionRepository.save(tx);
            }
            
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setAvailable(BigDecimal.ZERO);
            walletRepository.save(wallet);
        }
    }

    // 凍結資金 (下單時呼叫)
    @Transactional
    public void freezeFunds(Integer memberId, String coinId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        
        Wallet wallet = getWallet(memberId, coinId);
        if (wallet.getAvailable().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance for " + coinId);
        }
        
        wallet.setAvailable(wallet.getAvailable().subtract(amount));
        walletRepository.save(wallet);
    }

    // 解凍資金 (取消訂單或成交後釋放剩餘保證金)
    @Transactional
    public void unfreezeFunds(Integer memberId, String coinId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        Wallet wallet = getWallet(memberId, coinId);
        wallet.setAvailable(wallet.getAvailable().add(amount));
        walletRepository.save(wallet);
    }

    private Wallet createEmptyWallet(Integer memberId, String coinId) {
        // Double check existence (though deposit calls check first, getWallet might be called directly)
        if (!coinRepository.existsById(coinId)) {
             throw new IllegalArgumentException("Coin not found: " + coinId);
        }

        Wallet wallet = new Wallet();
        wallet.setMemberId(memberId);
        wallet.setCoinId(coinId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setAvailable(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }
}