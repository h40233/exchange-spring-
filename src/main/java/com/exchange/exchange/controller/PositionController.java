package com.exchange.exchange.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ====== 檔案總結 ======
// PositionController 提供合約倉位查詢的 RESTful API。
// 目前所有功能程式碼皆被註解 (Commented Out)，處於待實作或暫停狀態。
// 預期功能：
// 1. /api/positions/history: 查詢歷史倉位。
// 2. /api/positions/open: 查詢當前持倉。
@RestController
@RequestMapping("/api/positions")
public class PositionController {

    /* [暫時停用 - Pending Implementation]
    
    @Autowired
    private PositionRepository positionRepository;

    // 輔助方法：從 Session 獲取 Member ID
    private Integer getMemberId(HttpSession session) {
        return (Integer) session.getAttribute("memberId");
    }

    // API: 獲取倉位歷史 (包含已平倉)
    @GetMapping("/history")
    public ResponseEntity<?> getPositionHistory(HttpSession session) {
        Integer memberId = getMemberId(session);
        // 權限檢查：未登入則回傳 401
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Position> positions = positionRepository.findByMemberIdOrderByCloseAtDesc(memberId);
        return ResponseEntity.ok(positions);
    }

    // API: 獲取當前持倉 (僅 OPEN 狀態)
    @GetMapping("/open")
    public ResponseEntity<?> getOpenPositions(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // [註1] 效能優化建議：
        // 目前是先撈出所有歷史紀錄，再於記憶體中使用 Stream 過濾，這在資料量大時效率極差。
        // 應直接在 Repository 定義 findByMemberIdAndStatus(memberId, OPEN) 查詢。
        List<Position> all = positionRepository.findByMemberIdOrderByCloseAtDesc(memberId);
        List<Position> open = all.stream()
                .filter(p -> p.getStatus() == com.exchange.exchange.enums.PositionStatus.OPEN)
                .toList();
                
        return ResponseEntity.ok(open);
    }
    */
}