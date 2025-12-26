package com.exchange.exchange.service;

import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Member;
import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.MemberRepository;
import com.exchange.exchange.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testDeposit() {
        // 1. Ensure Coin exists
        String coinId = "USDT";
        if (!coinRepository.existsById(coinId)) {
            Coin coin = new Coin();
            coin.setCoinId(coinId);
            coin.setName("Tether");
            coin.setDecimals(18.0f);
            coinRepository.save(coin);
        }

        // 2. Create a test member
        Member member = new Member();
        member.setAccount("test_user_" + System.currentTimeMillis());
        member.setPassword("password");
        member.setName("Test User");
        member = memberRepository.save(member);
        Integer memberId = member.getMemberId();

        System.out.println("Testing deposit for Member: " + memberId + ", Coin: " + coinId);

        // 3. Perform Deposit
        BigDecimal amount = new BigDecimal("100.0");
        Wallet wallet = walletService.deposit(memberId, coinId, amount);

        // 4. Verify
        assertNotNull(wallet);
        assertEquals(0, amount.compareTo(wallet.getBalance()));
        assertEquals(memberId, wallet.getMemberId());
        assertEquals(coinId, wallet.getCoinId());

        System.out.println("Deposit successful. Balance: " + wallet.getBalance());
        
        // Cleanup (Optional, transaction rollback might handle it if configured, 
        // but explicit delete helps if not using @Transactional on test method)
        walletRepository.delete(wallet);
        memberRepository.delete(member);
    }
}
