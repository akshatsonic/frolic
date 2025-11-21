package com.frolic.services.service.budget;

import com.frolic.core.cache.store.RedisBudgetStore;
import com.frolic.core.repository.entity.GameBrandBudgetEntity;
import com.frolic.core.repository.jpa.GameBrandBudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for syncing budgets between Redis and PostgreSQL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetSyncService {
    
    private final RedisBudgetStore redisBudgetStore;
    private final GameBrandBudgetRepository budgetRepository;
    
    /**
     * Sync budgets from Redis to PostgreSQL for a game
     * This should be called when a game ends to persist the final budget state
     */
    @Transactional
    public void syncBudgetsFromRedisToPostgres(String gameId) {
        log.info("Starting budget sync from Redis to PostgreSQL for game: {}", gameId);
        
        List<GameBrandBudgetEntity> budgets = budgetRepository.findByGameId(gameId);
        
        for (GameBrandBudgetEntity budget : budgets) {
            try {
                // Get remaining budget from Redis
                Integer remainingBudget = redisBudgetStore.getRemainingBudget(gameId, budget.getBrandId());
                
                if (remainingBudget != null) {
                    // Calculate allocated budget
                    int allocatedBudget = budget.getTotalBudget() - remainingBudget;
                    
                    // Update PostgreSQL
                    budget.setAllocatedBudget(allocatedBudget);
                    budget.setRemainingBudget(remainingBudget);
                    
                    budgetRepository.save(budget);
                    
                    log.info("Synced budget for game={}, brand={}, total={}, allocated={}, remaining={}", 
                        gameId, budget.getBrandId(), budget.getTotalBudget(), allocatedBudget, remainingBudget);
                } else {
                    log.warn("No Redis budget found for game={}, brand={}, skipping sync", 
                        gameId, budget.getBrandId());
                }
            } catch (Exception e) {
                log.error("Failed to sync budget for game={}, brand={}", 
                    gameId, budget.getBrandId(), e);
            }
        }
        
        log.info("Completed budget sync from Redis to PostgreSQL for game: {}", gameId);
    }
    
    /**
     * Sync single brand budget from Redis to PostgreSQL
     */
    @Transactional
    public void syncBrandBudget(String gameId, String brandId) {
        log.info("Syncing budget for game={}, brand={}", gameId, brandId);
        
        GameBrandBudgetEntity budget = budgetRepository.findByGameIdAndBrandId(gameId, brandId)
            .orElseThrow(() -> new RuntimeException("Budget not found for game=" + gameId + ", brand=" + brandId));
        
        Integer remainingBudget = redisBudgetStore.getRemainingBudget(gameId, brandId);
        
        if (remainingBudget != null) {
            int allocatedBudget = budget.getTotalBudget() - remainingBudget;
            budget.setAllocatedBudget(allocatedBudget);
            budget.setRemainingBudget(remainingBudget);
            
            budgetRepository.save(budget);
            
            log.info("Synced budget for game={}, brand={}, allocated={}, remaining={}", 
                gameId, brandId, allocatedBudget, remainingBudget);
        }
    }
}
