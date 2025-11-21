package com.frolic.services.service.allocation;

import com.frolic.core.cache.store.RedisResultStore;
import com.frolic.core.common.dto.PlayEventDto;
import com.frolic.core.common.dto.PlayResultDto;
import com.frolic.core.common.enums.PlayStatus;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.engine.concurrency.AtomicBudgetDecrementer;
import com.frolic.core.engine.concurrency.IdempotencyHandler;
import com.frolic.core.engine.probability.ProbabilityCalculator;
import com.frolic.core.repository.entity.GameBrandBudgetEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.entity.PlayEventEntity;
import com.frolic.core.repository.jpa.GameBrandBudgetRepository;
import com.frolic.core.repository.jpa.GameRepository;
import com.frolic.core.repository.jpa.PlayEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service for reward allocation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardAllocationService {
    
    /**
     * Helper record to store brand budget information
     */
    private record BrandBudgetInfo(GameBrandBudgetEntity budget, int remainingBudget) {}
    
    private final IdempotencyHandler idempotencyHandler;
    private final ProbabilityCalculator probabilityCalculator;
    private final AtomicBudgetDecrementer budgetDecrementer;
    private final GameRepository gameRepository;
    private final GameBrandBudgetRepository gameBrandBudgetRepository;
    private final PlayEventRepository playEventRepository;
    private final RedisResultStore redisResultStore;
    private final Random random = new Random();
    
    /**
     * Process a play event and allocate rewards
     */
    @Transactional
    public void processPlayEvent(PlayEventDto event) {
        // Check idempotency
        if (!idempotencyHandler.tryMarkAsProcessed(event.getPlayId())) {
            log.warn("Play already processed: playId={}", event.getPlayId());
            return;
        }
        
        // Load game configuration
        GameEntity game = gameRepository.findById(event.getGameId())
            .orElseThrow(() -> new ResourceNotFoundException("Game", event.getGameId()));
        
        // Get brand budgets for this game
        List<GameBrandBudgetEntity> brandBudgets = gameBrandBudgetRepository.findByGameId(event.getGameId());
        
        if (brandBudgets.isEmpty()) {
            log.warn("No brand budgets configured for game: gameId={}", event.getGameId());
            createLoserResult(event, game);
            return;
        }
        
        // Check all brand budgets and filter those with remaining coupons
        List<BrandBudgetInfo> availableBrands = brandBudgets.stream()
            .map(budget -> {
                int remaining = budgetDecrementer.getCurrentBudget(event.getGameId(), budget.getBrandId());
                return new BrandBudgetInfo(budget, remaining);
            })
            .filter(info -> info.remainingBudget > 0)
            .collect(Collectors.toList());
        
        if (availableBrands.isEmpty()) {
            log.info("No remaining budget across all brands for game: gameId={}", event.getGameId());
            createLoserResult(event, game);
            return;
        }
        
        // Calculate TOTAL remaining budget across all brands
        int totalRemainingBudget = availableBrands.stream()
            .mapToInt(info -> info.remainingBudget)
            .sum();
        
        log.debug("Total game budget: totalBudget={}, availableBrands={}", 
            totalRemainingBudget, availableBrands.size());
        
        // Step 1: Calculate allocation based on TOTAL game budget
        int allocation = probabilityCalculator.calculateAllocation(
            totalRemainingBudget,
            game.getStartTime(),
            game.getEndTime(),
            game.getSlotGranularitySeconds()
        );
        
        // Step 2: If user wins, select a random brand and allocate
        if (allocation > 0) {
            // Randomly select a brand from those with available budget
            BrandBudgetInfo selectedBrandInfo = availableBrands.get(random.nextInt(availableBrands.size()));
            GameBrandBudgetEntity selectedBudget = selectedBrandInfo.budget;
            
            log.debug("User won! Selected brand: brandId={}, brandBudget={}", 
                selectedBudget.getBrandId(), selectedBrandInfo.remainingBudget);
            
            // For winner, we allocate exactly 1 coupon from the selected brand
            // (allocation quantity is based on total budget, but we give 1 coupon per win)
            boolean success = budgetDecrementer.decrementBudget(
                event.getGameId(),
                selectedBudget.getBrandId(),
                1  // Allocate 1 coupon from selected brand
            );
            
            if (success) {
                createWinnerResult(event, game, selectedBudget, 1);
            } else {
                log.info("Failed to decrement budget (race condition): playId={}", event.getPlayId());
                createLoserResult(event, game);
            }
        } else {
            createLoserResult(event, game);
        }
    }
    
    private void createWinnerResult(PlayEventDto event, GameEntity game, GameBrandBudgetEntity budget, int allocation) {
        PlayResultDto result = PlayResultDto.builder()
            .playId(event.getPlayId())
            .gameId(event.getGameId())
            .userId(event.getUserId())
            .winner(true)
            .couponId(java.util.UUID.randomUUID().toString()) // Simplified - would fetch actual coupon
            .brandId(budget.getBrandId())
            .brandName("Brand-" + budget.getBrandId()) // Simplified - would fetch from DB
            .timestamp(Instant.now())
            .message("Congratulations! You won " + allocation + " coupon(s)!")
            .build();
        
        // Store in Redis
        redisResultStore.storeResult(result);
        
        // Save to database
        PlayEventEntity playEntity = new PlayEventEntity();
        playEntity.setId(event.getPlayId());
        playEntity.setGameId(event.getGameId());
        playEntity.setUserId(event.getUserId());
        playEntity.setStatus(PlayStatus.WINNER);
        playEntity.setTimestamp(event.getTimestamp());
        playEntity.setWinner(true);
        playEntity.setBrandId(budget.getBrandId());
        playEntity.setCouponId(result.getCouponId());
        playEventRepository.save(playEntity);
        
        log.info("Winner: playId={}, userId={}, brandId={}, allocation={}", 
            event.getPlayId(), event.getUserId(), budget.getBrandId(), allocation);
    }
    
    private void createLoserResult(PlayEventDto event, GameEntity game) {
        PlayResultDto result = PlayResultDto.builder()
            .playId(event.getPlayId())
            .gameId(event.getGameId())
            .userId(event.getUserId())
            .winner(false)
            .timestamp(Instant.now())
            .message("Better luck next time!")
            .build();
        
        // Store in Redis
        redisResultStore.storeResult(result);
        
        // Save to database
        PlayEventEntity playEntity = new PlayEventEntity();
        playEntity.setId(event.getPlayId());
        playEntity.setGameId(event.getGameId());
        playEntity.setUserId(event.getUserId());
        playEntity.setStatus(PlayStatus.LOSER);
        playEntity.setTimestamp(event.getTimestamp());
        playEntity.setWinner(false);
        playEventRepository.save(playEntity);
        
        log.info("Loser: playId={}, userId={}", event.getPlayId(), event.getUserId());
    }
}
