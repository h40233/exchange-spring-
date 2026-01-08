package com.exchange.exchange.service;

// 引入實體：倉位
import com.exchange.exchange.entity.Position;
// 引入枚舉：訂單方向、倉位方向、倉位狀態
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.PositionSide;
import com.exchange.exchange.enums.PositionStatus;
// 引入資料存取層
import com.exchange.exchange.repository.PositionRepository;
// 引入 Spring 工具
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

// ====== 檔案總結 ======
// PositionService 負責管理合約交易的倉位生命週期。
// 核心職責：
// 1. 判斷交易是「開倉」還是「平倉」。
// 2. 加倉時：重新計算加權平均價格 (Average Entry Price)。
// 3. 減倉/平倉時：計算並實現盈虧 (Realized PnL)，更新剩餘持倉。
// 4. 反向開倉 (Flip)：若平倉數量大於持倉，則自動轉為反向持倉。
@Service
public class PositionService {

    // 注入倉位資料庫操作介面
    @Autowired
    private PositionRepository positionRepository;

    // 注入錢包服務，用於結算盈虧至餘額
    @Autowired
    private WalletService walletService;

    // 方法：處理成交後的倉位變動
    // 參數：會員ID, 交易對, 成交方向 (買/賣), 成交價, 成交量
    // 使用 @Transactional 確保倉位更新與錢包結算的一致性
    @Transactional
    public void processTrade(Integer memberId, String symbolId, OrderSide orderSide, BigDecimal price, BigDecimal quantity) {
        // 嘗試查找該會員在該幣對下「狀態為 OPEN」的現有倉位
        // 假設：每個幣對同時只能有一個方向的持倉 (單向持倉模式 One-Way Mode) [註1]
        Optional<Position> openPosOpt = positionRepository.findByMemberIdAndSymbolIdAndStatus(memberId, symbolId, PositionStatus.OPEN);

        if (openPosOpt.isEmpty()) {
            // 若無持倉 -> 視為新開倉 (Open New Position)
            openNewPosition(memberId, symbolId, orderSide, price, quantity);
        } else {
            // 若已有持倉 -> 進行更新 (可能為加倉或減倉)
            Position position = openPosOpt.get();
            updatePosition(position, orderSide, price, quantity);
        }
    }

    // 私有方法：建立新倉位
    private void openNewPosition(Integer memberId, String symbolId, OrderSide orderSide, BigDecimal price, BigDecimal quantity) {
        Position pos = new Position();
        pos.setMemberId(memberId);
        pos.setSymbolId(symbolId);
        pos.setStatus(PositionStatus.OPEN);
        pos.setOpenAt(LocalDateTime.now());
        pos.setCloseAt(pos.getOpenAt()); // 初始化結束時間 (避免資料庫 NOT NULL 錯誤)
        pos.setQuantity(quantity);
        pos.setAvgprice(price);          // 初始均價即為本次成交價
        pos.setPnl(BigDecimal.ZERO);     // 初始已實現盈虧為 0

        // 轉換訂單方向為倉位方向
        // 買入開倉 -> 做多 (LONG)
        // 賣出開倉 -> 做空 (SHORT)
        if (orderSide == OrderSide.BUY) {
            pos.setSide(PositionSide.LONG);
        } else {
            pos.setSide(PositionSide.SHORT);
        }
        
        positionRepository.save(pos);
    }

    // 私有方法：更新現有倉位 (加倉或平倉邏輯)
    private void updatePosition(Position position, OrderSide orderSide, BigDecimal price, BigDecimal quantity) {
        // 判斷是否為同方向交易 (加倉)
        // 原持倉 LONG + 買入 = 加倉
        // 原持倉 SHORT + 賣出 = 加倉
        boolean isSameSide = (position.getSide() == PositionSide.LONG && orderSide == OrderSide.BUY) ||
                             (position.getSide() == PositionSide.SHORT && orderSide == OrderSide.SELL);

        if (isSameSide) {
            // === 加倉邏輯 (Increase Position) ===
            // 計算新均價公式：((舊數量 * 舊均價) + (新數量 * 新價格)) / (舊數量 + 新數量)
            BigDecimal totalCost = position.getQuantity().multiply(position.getAvgprice())
                    .add(quantity.multiply(price));
            BigDecimal totalQty = position.getQuantity().add(quantity);
            // 除法運算需指定精度與捨入模式 (18位小數, 四雪五入)
            BigDecimal newAvg = totalCost.divide(totalQty, 18, RoundingMode.HALF_UP);

            // 更新倉位資訊
            position.setQuantity(totalQty);
            position.setAvgprice(newAvg);
            positionRepository.save(position);
        } else {
            // === 減倉/平倉邏輯 (Reduce / Close Position) ===
            BigDecimal currentQty = position.getQuantity();

            if (quantity.compareTo(currentQty) >= 0) {
                // 情境 1: 完全平倉 或 反向開倉 (Full Close or Flip)
                // 1. 先將當前持倉完全平掉
                BigDecimal pnl = calculatePnL(position.getSide(), position.getAvgprice(), price, currentQty);
                
                position.setQuantity(BigDecimal.ZERO);
                position.setStatus(PositionStatus.CLOSED);
                position.setCloseAt(LocalDateTime.now());
                // 累加本次平倉產生的盈虧到歷史紀錄
                position.setPnl(position.getPnl().add(pnl));
                positionRepository.save(position);

                // 結算盈虧至錢包 (Realize PnL to Wallet)
                // 簡化邏輯：假設報價幣皆為 USDT (例如 BTCUSDT)
                String quoteCoin = position.getSymbolId().endsWith("USDT") ? "USDT" : "USDT"; 
                walletService.realizePnL(position.getMemberId(), quoteCoin, pnl);
                
                // 2. 處理反向開倉 (Flip)
                // 若本次下單數量 > 原持倉數量，剩餘部分視為反向新開倉
                BigDecimal remaining = quantity.subtract(currentQty);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    openNewPosition(position.getMemberId(), position.getSymbolId(), orderSide, price, remaining);
                }

            } else {
                // 情境 2: 部分平倉 (Partial Close)
                // 計算本次部分平倉的盈虧
                BigDecimal pnl = calculatePnL(position.getSide(), position.getAvgprice(), price, quantity);
                
                // 更新剩餘持倉數量 (均價不變)
                position.setQuantity(currentQty.subtract(quantity));
                
                // 記錄已實現盈虧
                // 注意：這裡將已實現盈虧存入 `pnl` 欄位，這意味著該欄位代表「累計已實現盈虧」
                position.setPnl(position.getPnl().add(pnl));
                positionRepository.save(position);

                // 同步將盈虧金額反映到錢包餘額
                String quoteCoin = position.getSymbolId().endsWith("USDT") ? "USDT" : "USDT";
                walletService.realizePnL(position.getMemberId(), quoteCoin, pnl);
            }
        }
    }

    // 私有方法：計算盈虧 (Calculate PnL)
    private BigDecimal calculatePnL(PositionSide side, BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal qty) {
        // 做多 (LONG): (平倉價 - 開倉價) * 數量
        // 做空 (SHORT): (開倉價 - 平倉價) * 數量
        if (side == PositionSide.LONG) {
            return exitPrice.subtract(entryPrice).multiply(qty);
        } else {
            return entryPrice.subtract(exitPrice).multiply(qty);
        }
    }
}

// ====== 備註區 ======
/*
[註1] 雙向持倉 (Hedge Mode):
      目前的實作採用單向持倉模式 (One-Way Mode)，即同一個幣對只能選擇做多或做空。
      若要支援雙向持倉 (如同時持有 BTC Long 與 BTC Short)，需在 Position 表與查詢邏輯中加入 PositionSide 條件。

[註2] 盈虧欄位定義:
      目前的 `pnl` 欄位混合了「歷史已實現」的概念。
      在部分平倉時，`avgprice` 不變，但 `pnl` 增加，這在顯示「未實現盈虧」(Unrealized PnL) 時可能會造成混淆。
      建議將 `realized_pnl` 與 `unrealized_pnl` 分開，或僅在平倉歷史表中記錄 PnL。

[註3] 貨幣解析 (Currency Parsing):
      `endsWith("USDT")` 是硬編碼邏輯。
      建議透過 SymbolRepository 查詢該 Symbol 的 `quoteCoinId`，以支援如 BTC/ETH 等非 USDT 交易對。
*/