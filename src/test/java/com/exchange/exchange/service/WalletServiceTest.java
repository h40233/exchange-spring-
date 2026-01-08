package com.exchange.exchange.service;

// 引入實體與存取層
import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Member;
import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.MemberRepository;
import com.exchange.exchange.repository.WalletRepository;
// 引入 JUnit 5 與 Spring Test
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// ====== 檔案總結 ======
// WalletServiceTest 是一個整合測試類別。
// 它會啟動完整的 Spring Context 來測試 WalletService 與資料庫的交互。
@SpringBootTest
public class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WalletRepository walletRepository;

    // 測試案例：驗證儲值 (Deposit) 功能
    // 流程：建立測試幣種 -> 建立測試會員 -> 執行儲值 -> 驗證餘額
    @Test
    public void testDeposit() {
        // 1. 確保測試用的幣種 (USDT) 存在
        String coinId = "USDT";
        if (!coinRepository.existsById(coinId)) {
            Coin coin = new Coin();
            coin.setCoinId(coinId);
            coin.setName("Tether");
            coin.setDecimals(18.0f);
            coinRepository.save(coin);
        }

        // 2. 建立一個臨時的測試會員
        Member member = new Member();
        // 使用時間戳記避免帳號重複衝突
        member.setAccount("test_user_" + System.currentTimeMillis());
        member.setPassword("password");
        member.setName("Test User");
        member = memberRepository.save(member);
        Integer memberId = member.getMemberId();

        System.out.println("Testing deposit for Member: " + memberId + ", Coin: " + coinId);

        // 3. 執行儲值操作 (核心測試目標)
        BigDecimal amount = new BigDecimal("100.0");
        Wallet wallet = walletService.deposit(memberId, coinId, amount);

        // 4. 驗證結果 (Assertions)
        // 確認回傳的錢包物件不為空
        assertNotNull(wallet);
        // 確認餘額是否等於預期 (使用 compareTo 比較 BigDecimal)
        assertEquals(0, amount.compareTo(wallet.getBalance()));
        // 確認錢包歸屬與幣種正確
        assertEquals(memberId, wallet.getMemberId());
        assertEquals(coinId, wallet.getCoinId());

        System.out.println("Deposit successful. Balance: " + wallet.getBalance());
        
        // 清理資料 (Cleanup)
        // 雖然測試方法結束後資料仍會留在資料庫 (除非在類別或方法上加 @Transactional)，
        // 但這裡手動刪除是良好的習慣，避免污染下次測試。
        // [註1] 更好的做法是直接在 @Test 方法上加上 @Transactional，
        // 這樣測試結束後 Spring 會自動 rollback 所有資料庫變更。
        walletRepository.delete(wallet);
        memberRepository.delete(member);
    }
}