package com.exchange.exchange.repository;

import com.exchange.exchange.entity.Position;
import com.exchange.exchange.enums.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {
    List<Position> findByMemberIdOrderByCloseAtDesc(Integer memberId);
    
    Optional<Position> findByMemberIdAndSymbolIdAndStatus(Integer memberId, String symbolId, PositionStatus status);
}
