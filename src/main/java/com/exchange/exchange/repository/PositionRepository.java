package com.exchange.exchange.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.entity.Position;
import com.exchange.exchange.enums.PositionStatus;

// ====== 檔案總結 ======
// PositionRepository 負責對 `positions` 表進行操作。
// 用於查詢使用者的合約持倉狀態。
@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {
    
    // 查詢會員的所有歷史倉位 (包含已平倉)
    List<Position> findByMemberIdOrderByCloseAtDesc(Integer memberId);
    
    // 查詢會員在特定交易對中，處於特定狀態 (如 OPEN) 的倉位
    // 用於判斷是否需要加倉或平倉
    Optional<Position> findByMemberIdAndSymbolIdAndStatus(Integer memberId, String symbolId, PositionStatus status);
}