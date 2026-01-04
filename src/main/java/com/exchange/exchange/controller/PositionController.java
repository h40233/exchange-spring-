package com.exchange.exchange.controller;

import com.exchange.exchange.entity.Position;
import com.exchange.exchange.repository.PositionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    /*
    @Autowired
    private PositionRepository positionRepository;

    private Integer getMemberId(HttpSession session) {
        return (Integer) session.getAttribute("memberId");
    }

    @GetMapping("/history")
    public ResponseEntity<?> getPositionHistory(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Position> positions = positionRepository.findByMemberIdOrderByCloseAtDesc(memberId);
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/open")
    public ResponseEntity<?> getOpenPositions(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Position> all = positionRepository.findByMemberIdOrderByCloseAtDesc(memberId);
        List<Position> open = all.stream()
                .filter(p -> p.getStatus() == com.exchange.exchange.enums.PositionStatus.OPEN)
                .toList();
                
        return ResponseEntity.ok(open);
    }
    */
}
