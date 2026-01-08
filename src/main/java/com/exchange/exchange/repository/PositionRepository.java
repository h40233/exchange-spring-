package com.exchange.exchange.repository;

// 引入實體與枚舉
import com.exchange.exchange.entity.Position;
import com.exchange.exchange.enums.PositionStatus;
// 引入 JPA
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ====== 檔案總結 ======
// PositionRepository 負責倉位資料的存取。
// 支援查詢歷史倉位與查找特定交易對的當前持倉。
@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {
    
    // 查詢某會員的所有倉位紀錄 (包含已平倉)，依平倉時間倒序排列
    List<Position> findByMemberIdOrderByCloseAtDesc(Integer memberId);
    
    // 查詢某會員在特定交易對的特定狀態倉位
    // 通常用於查找 "OPEN" 狀態的倉位，以進行加倉或平倉操作
    // 假設：單一交易對在同一時間只能有一個 OPEN 倉位 (單向持倉模式)
    Optional<Position> findByMemberIdAndSymbolIdAndStatus(Integer memberId, String symbolId, PositionStatus status);
}