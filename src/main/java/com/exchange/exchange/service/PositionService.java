package com.exchange.exchange.service;

import com.exchange.exchange.entity.Position;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.PositionSide;
import com.exchange.exchange.enums.PositionStatus;
import com.exchange.exchange.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PositionService {

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private WalletService walletService;

    @Transactional
    public void processTrade(Integer memberId, String symbolId, OrderSide orderSide, BigDecimal price, BigDecimal quantity) {
        // Find any existing OPEN position
        Optional<Position> openPosOpt = positionRepository.findByMemberIdAndSymbolIdAndStatus(memberId, symbolId, PositionStatus.OPEN);

        if (openPosOpt.isEmpty()) {
            // No position -> Open new
            openNewPosition(memberId, symbolId, orderSide, price, quantity);
        } else {
            // Existing position -> Update
            Position position = openPosOpt.get();
            updatePosition(position, orderSide, price, quantity);
        }
    }

    private void openNewPosition(Integer memberId, String symbolId, OrderSide orderSide, BigDecimal price, BigDecimal quantity) {
        Position pos = new Position();
        pos.setMemberId(memberId);
        pos.setSymbolId(symbolId);
        pos.setStatus(PositionStatus.OPEN);
        pos.setOpenAt(LocalDateTime.now());
        pos.setCloseAt(pos.getOpenAt()); // Initialize to avoid NOT NULL error
        pos.setQuantity(quantity);
        pos.setAvgprice(price);
        pos.setPnl(BigDecimal.ZERO);

        // BUY -> LONG, SELL -> SHORT
        if (orderSide == OrderSide.BUY) {
            pos.setSide(PositionSide.LONG);
        } else {
            pos.setSide(PositionSide.SHORT);
        }
        
        positionRepository.save(pos);
    }

    private void updatePosition(Position position, OrderSide orderSide, BigDecimal price, BigDecimal quantity) {
        boolean isSameSide = (position.getSide() == PositionSide.LONG && orderSide == OrderSide.BUY) ||
                             (position.getSide() == PositionSide.SHORT && orderSide == OrderSide.SELL);

        if (isSameSide) {
            // Increase Position
            // New Avg Price = ((OldQty * OldAvg) + (NewQty * NewPrice)) / (OldQty + NewQty)
            BigDecimal totalCost = position.getQuantity().multiply(position.getAvgprice())
                    .add(quantity.multiply(price));
            BigDecimal totalQty = position.getQuantity().add(quantity);
            BigDecimal newAvg = totalCost.divide(totalQty, 18, RoundingMode.HALF_UP);

            position.setQuantity(totalQty);
            position.setAvgprice(newAvg);
            positionRepository.save(position);
        } else {
            // Reduce / Close Position
            BigDecimal currentQty = position.getQuantity();

            if (quantity.compareTo(currentQty) >= 0) {
                // Full Close (or Flip)
                // 1. Close current position fully
                BigDecimal pnl = calculatePnL(position.getSide(), position.getAvgprice(), price, currentQty);
                
                position.setQuantity(BigDecimal.ZERO);
                position.setStatus(PositionStatus.CLOSED);
                position.setCloseAt(LocalDateTime.now());
                position.setPnl(position.getPnl().add(pnl));
                positionRepository.save(position);

                // Realize PnL to Wallet (Assuming Quote Coin is USDT usually, extracting from SymbolID could be better but simplified here)
                // Standard symbol format: BTCUSDT -> Quote is USDT.
                String quoteCoin = position.getSymbolId().endsWith("USDT") ? "USDT" : "USDT"; // Simplified
                walletService.realizePnL(position.getMemberId(), quoteCoin, pnl);
                
                // 2. Handle Flip (Remaining Qty)
                BigDecimal remaining = quantity.subtract(currentQty);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    // Open opposite position
                    openNewPosition(position.getMemberId(), position.getSymbolId(), orderSide, price, remaining);
                }

            } else {
                // Partial Close
                BigDecimal pnl = calculatePnL(position.getSide(), position.getAvgprice(), price, quantity);
                
                // Update existing position (AvgPrice stays same)
                position.setQuantity(currentQty.subtract(quantity));
                // Add PnL to history? Position entity has one PnL field. 
                // Usually realized PnL is tracked separately or accumulated. 
                // Here we accumulate it on the position record until it closes, 
                // BUT if we partially close, that PnL is realized.
                // We should probably log it or just add to wallet. 
                // We will add to wallet AND accumulate in 'pnl' field for record keeping?
                // If we accumulate, it might be confusing if PnL field implies "Unrealized".
                // Let's assume 'pnl' field in DB is "Realized PnL".
                
                position.setPnl(position.getPnl().add(pnl));
                positionRepository.save(position);

                String quoteCoin = position.getSymbolId().endsWith("USDT") ? "USDT" : "USDT";
                walletService.realizePnL(position.getMemberId(), quoteCoin, pnl);
            }
        }
    }

    private BigDecimal calculatePnL(PositionSide side, BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal qty) {
        // LONG: (Exit - Entry) * Qty
        // SHORT: (Entry - Exit) * Qty
        if (side == PositionSide.LONG) {
            return exitPrice.subtract(entryPrice).multiply(qty);
        } else {
            return entryPrice.subtract(exitPrice).multiply(qty);
        }
    }
}
