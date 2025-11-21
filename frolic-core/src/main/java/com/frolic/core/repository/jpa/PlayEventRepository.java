package com.frolic.core.repository.jpa;

import com.frolic.core.common.enums.PlayStatus;
import com.frolic.core.repository.entity.PlayEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for PlayEvent entity
 */
@Repository
public interface PlayEventRepository extends JpaRepository<PlayEventEntity, String> {
    
    List<PlayEventEntity> findByGameId(String gameId);
    
    List<PlayEventEntity> findByUserId(String userId);
    
    List<PlayEventEntity> findByGameIdAndStatus(String gameId, PlayStatus status);
    
    @Query("SELECT COUNT(p) FROM PlayEventEntity p WHERE p.gameId = :gameId AND p.winner = true")
    long countWinnersByGameId(@Param("gameId") String gameId);
    
    @Query("SELECT COUNT(p) FROM PlayEventEntity p WHERE p.gameId = :gameId AND p.timestamp BETWEEN :start AND :end")
    long countPlaysInTimeRange(@Param("gameId") String gameId, @Param("start") Instant start, @Param("end") Instant end);
}
