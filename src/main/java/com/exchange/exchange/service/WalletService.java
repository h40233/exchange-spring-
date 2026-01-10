package com.exchange.exchange.service;

// 引入實體：錢包、錢包主鍵 (複合鍵)
import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.entity.key.WalletId;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.WalletRepository;

// ====== 檔案總結 ======
// WalletService 管理使用者的資產狀態。
// 核心概念：
// 1. Balance (總餘額)：使用者擁有的資產總量 (包含凍結中)。
// 2. Available (可用餘額)：可以自由使用的資產 (總餘額 - 凍結金額)。
// 所有的資金變動都必須透過此 Service，且應嚴格遵守事務原子性。
@Service
public class WalletService {

    // 注入錢包資料庫介面
    @Autowired
    private WalletRepository walletRepository;

    // 注入幣種資料庫介面，用於驗證幣種是否存在
    @Autowired
    private CoinRepository coinRepository;

    // 注入交易流水紀錄介面
    @Autowired
    private com.exchange.exchange.repository.WalletTransactionRepository transactionRepository;

    // 方法：取得某會員所有幣種的錢包
    public List<Wallet> getWallets(Integer memberId) {
        return walletRepository.findByMemberId(memberId);
    }

    // 方法：取得某會員的資金流水紀錄 (倒序排列)
    public List<com.exchange.exchange.entity.WalletTransaction> getTransactions(Integer memberId) {
        return transactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    // 方法：取得單一錢包，若不存在則自動創建一個空錢包 (Lazy Creation)
    public Wallet getWallet(Integer memberId, String coinId) {
        // 清理輸入字串
        String cleanCoinId = coinId != null ? coinId.trim() : null;
        // 使用複合主鍵查詢，若無則呼叫 createEmptyWallet
        return walletRepository.findById(new WalletId(memberId, cleanCoinId))
                .orElseGet(() -> createEmptyWallet(memberId, cleanCoinId));
    }

    // 方法：模擬儲值 (Deposit)
    // 必須使用 @Transactional 確保錢包餘額更新與流水寫入同時成功
    @Transactional
    public Wallet deposit(Integer memberId, String coinId, BigDecimal amount) {
        // 驗證金額必須為正數
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        String cleanCoinId = coinId != null ? coinId.trim() : null;
        if (cleanCoinId == null || cleanCoinId.isEmpty()) {
            throw new IllegalArgumentException("Coin ID cannot be empty");
        }

        // 驗證幣種是否存在於系統配置中
        if (!coinRepository.existsById(cleanCoinId)) {
            throw new IllegalArgumentException("Coin not found: " + cleanCoinId);
        }

        // 獲取錢包物件
        Wallet wallet = getWallet(memberId, cleanCoinId);
        
        // 執行加款：同時增加總餘額與可用餘額
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setAvailable(wallet.getAvailable().add(amount));
        
        // 記錄資金流水 (Log transaction)
        com.exchange.exchange.entity.WalletTransaction tx = new com.exchange.exchange.entity.WalletTransaction(
            memberId, cleanCoinId, "DEPOSIT", amount
        );
        transactionRepository.save(tx);

        return walletRepository.save(wallet);
    }

    // 方法：結算盈虧 (Realize PnL) - 用於合約或平倉
    // PnL (Profit and Loss) 可以是正數(獲利)或負數(虧損)
    @Transactional
    public void realizePnL(Integer memberId, String coinId, BigDecimal pnlAmount) {
        // 若盈虧為 0 則不處理
        if (pnlAmount.compareTo(BigDecimal.ZERO) == 0) return;

        Wallet wallet = getWallet(memberId, coinId);
        
        // 更新餘額：直接將 PnL 加到 Balance 與 Available 上
        // 若 pnlAmount 為負，則 add 相當於減法
        wallet.setBalance(wallet.getBalance().add(pnlAmount));
        wallet.setAvailable(wallet.getAvailable().add(pnlAmount));
        
        walletRepository.save(wallet);

        // 記錄流水
        com.exchange.exchange.entity.WalletTransaction tx = new com.exchange.exchange.entity.WalletTransaction(
            memberId, coinId, "REALIZED_PNL", pnlAmount
        );
        transactionRepository.save(tx);
    }

    // 方法：扣除已凍結資金 (Deduct Frozen Funds) - 用於成交後的實際花費
    // 注意：因為下單時資金已經從 Available 扣除(凍結)了，所以這裡只扣除 Balance
    @Transactional
    public void deductFrozen(Integer memberId, String coinId, BigDecimal amount, String type) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        
        Wallet wallet = getWallet(memberId, coinId);
        // Balance 減少 (實際花費)，Available 不變 (因為先前凍結時已經扣過了)
        wallet.setBalance(wallet.getBalance().subtract(amount));
        
        walletRepository.save(wallet);
        
        // 記錄流水，金額記為負數
        transactionRepository.save(new com.exchange.exchange.entity.WalletTransaction(
            memberId, coinId, type, amount.negate()
        ));
    }

    // 方法：增加餘額 (Add Balance) - 用於成交後獲得資產
    // 例如買到 BTC，則 BTC 錢包同時增加 Balance 與 Available
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

    // 方法：重置所有錢包 (測試用功能)
    @Transactional
    public void resetWallets(Integer memberId) {
        List<Wallet> wallets = walletRepository.findByMemberId(memberId);
        for (Wallet wallet : wallets) {
            BigDecimal oldBalance = wallet.getBalance();
            if (oldBalance.compareTo(BigDecimal.ZERO) > 0) {
                // 記錄一筆 "RESET" 的取出流水
                 com.exchange.exchange.entity.WalletTransaction tx = new com.exchange.exchange.entity.WalletTransaction(
                    memberId, wallet.getCoinId(), "WITHDRAW (RESET)", oldBalance.negate()
                );
                transactionRepository.save(tx);
            }
            
            // 歸零
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setAvailable(BigDecimal.ZERO);
            walletRepository.save(wallet);
        }
    }

    // 方法：凍結資金 (Freeze Funds) - 下單時呼叫
    // 將資金從 Available 移出，但保留在 Balance 中 (隱含的 Frozen = Balance - Available)
    @Transactional
    public void freezeFunds(Integer memberId, String coinId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        
        Wallet wallet = getWallet(memberId, coinId);
        // 檢查可用餘額是否足夠
        if (wallet.getAvailable().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance for " + coinId);
        }
        
        // 扣除可用餘額 (Available decreases, Balance stays same)
        wallet.setAvailable(wallet.getAvailable().subtract(amount));
        walletRepository.save(wallet);
    }

    // 方法：解凍資金 (Unfreeze Funds) - 取消訂單或退還多餘保證金時呼叫
    // 將資金加回 Available，Balance 不變
    @Transactional
    public void unfreezeFunds(Integer memberId, String coinId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        Wallet wallet = getWallet(memberId, coinId);
        // 資金回補至可用餘額
        wallet.setAvailable(wallet.getAvailable().add(amount));
        walletRepository.save(wallet);
    }

    // 私有方法：建立空錢包
    private Wallet createEmptyWallet(Integer memberId, String coinId) {
        // 再次檢查幣種是否存在
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

// ====== 備註區 ======
/*
[註1] 帳戶邏輯 (Accounting Logic):
      本系統採用「隱式凍結」邏輯：Frozen = Balance - Available。
      這意味著在資料庫中並不直接存儲 `frozen` 欄位，而是透過 `balance` (總資產) 與 `available` (可用資產) 的差額來推算。
      這是一個常見的設計，優點是欄位少，缺點是若 `balance` 與 `available` 更新不一致會導致帳務錯誤。
*/