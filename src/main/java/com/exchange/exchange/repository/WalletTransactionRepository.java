package com.exchange.exchange.repository;

import com.exchange.exchange.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByMemberIdOrderByCreatedAtDesc(Integer memberId);
}
