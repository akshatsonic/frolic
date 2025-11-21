package com.frolic.core.repository.jpa;

import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.repository.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Game entity
 */
@Repository
public interface GameRepository extends JpaRepository<GameEntity, String> {
    
    List<GameEntity> findByCampaignId(String campaignId);
    
    List<GameEntity> findByStatus(GameStatus status);
    
    @Query("SELECT g FROM GameEntity g WHERE g.status = :status AND g.startTime <= :now AND g.endTime >= :now")
    List<GameEntity> findActiveGames(@Param("status") GameStatus status, @Param("now") LocalDateTime now);
    
    List<GameEntity> findByStatusOrderByCreatedAtDesc(GameStatus status);
    
    @Query("SELECT g FROM GameEntity g WHERE g.status = com.frolic.core.common.enums.GameStatus.DRAFT AND g.startTime <= :now")
    List<GameEntity> findDraftGamesReadyToStart(@Param("now") LocalDateTime now);
    
    @Query("SELECT g FROM GameEntity g WHERE g.status = com.frolic.core.common.enums.GameStatus.ACTIVE AND g.endTime < :now")
    List<GameEntity> findActiveGamesReadyToEnd(@Param("now") LocalDateTime now);
}
