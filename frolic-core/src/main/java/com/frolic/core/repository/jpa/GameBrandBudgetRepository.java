package com.frolic.core.repository.jpa;

import com.frolic.core.repository.entity.GameBrandBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GameBrandBudget entity
 */
@Repository
public interface GameBrandBudgetRepository extends JpaRepository<GameBrandBudgetEntity, String> {
    
    List<GameBrandBudgetEntity> findByGameId(String gameId);
    
    Optional<GameBrandBudgetEntity> findByGameIdAndBrandId(String gameId, String brandId);
    
    @Modifying
    @Query("UPDATE GameBrandBudgetEntity g SET g.allocatedBudget = g.allocatedBudget + :amount WHERE g.gameId = :gameId AND g.brandId = :brandId")
    int incrementAllocatedBudget(@Param("gameId") String gameId, @Param("brandId") String brandId, @Param("amount") int amount);
}
