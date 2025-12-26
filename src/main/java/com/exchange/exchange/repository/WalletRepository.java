package com.exchange.exchange.repository;

import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.entity.key.WalletId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, WalletId> {
    List<Wallet> findByMemberId(Integer memberId);
}
