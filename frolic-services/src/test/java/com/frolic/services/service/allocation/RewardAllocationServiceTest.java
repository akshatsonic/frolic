package com.frolic.services.service.allocation;

import com.frolic.core.cache.store.RedisResultStore;
import com.frolic.core.common.dto.PlayEventDto;
import com.frolic.core.common.enums.PlayStatus;
import com.frolic.core.engine.concurrency.AtomicBudgetDecrementer;
import com.frolic.core.engine.concurrency.IdempotencyHandler;
import com.frolic.core.engine.probability.ProbabilityCalculator;
import com.frolic.core.repository.entity.GameBrandBudgetEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.entity.PlayEventEntity;
import com.frolic.core.repository.jpa.GameBrandBudgetRepository;
import com.frolic.core.repository.jpa.GameRepository;
import com.frolic.core.repository.jpa.PlayEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RewardAllocationService
 */
@ExtendWith(MockitoExtension.class)
class RewardAllocationServiceTest {
    
    @Mock
    private IdempotencyHandler idempotencyHandler;
    
    @Mock
    private ProbabilityCalculator probabilityCalculator;
    
    @Mock
    private AtomicBudgetDecrementer budgetDecrementer;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private GameBrandBudgetRepository gameBrandBudgetRepository;
    
    @Mock
    private PlayEventRepository playEventRepository;
    
    @Mock
    private RedisResultStore redisResultStore;
    
    private RewardAllocationService rewardAllocationService;
    
    @BeforeEach
    void setUp() {
        rewardAllocationService = new RewardAllocationService(
            idempotencyHandler,
            probabilityCalculator,
            budgetDecrementer,
            gameRepository,
            gameBrandBudgetRepository,
            playEventRepository,
            redisResultStore
        );
    }
    
    @Test
    void testProcessPlayEvent_AlreadyProcessed_Skips() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(false);
        
        rewardAllocationService.processPlayEvent(event);
        
        verify(gameRepository, never()).findById(anyString());
        verify(redisResultStore, never()).storeResult(any());
    }
    
    @Test
    void testProcessPlayEvent_NoBrandBudgets_CreatesLoserResult() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        GameEntity game = createGame("game-456", "campaign-123");
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(gameBrandBudgetRepository.findByGameId("game-456")).thenReturn(Collections.emptyList());
        
        rewardAllocationService.processPlayEvent(event);
        
        verify(redisResultStore).storeResult(argThat(result -> 
            !result.isWinner() && result.getPlayId().equals("play-123")
        ));
        verify(playEventRepository).save(argThat(playEntity -> 
            !playEntity.isWinner() && playEntity.getStatus() == PlayStatus.LOSER
        ));
    }
    
    @Test
    void testProcessPlayEvent_NoRemainingBudget_CreatesLoserResult() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        GameEntity game = createGame("game-456", "campaign-123");
        GameBrandBudgetEntity budget = createBrandBudget("game-456", "brand-1", 100);
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(gameBrandBudgetRepository.findByGameId("game-456"))
            .thenReturn(Collections.singletonList(budget));
        when(budgetDecrementer.getCurrentBudget("game-456", "brand-1")).thenReturn(0);
        
        rewardAllocationService.processPlayEvent(event);
        
        verify(redisResultStore).storeResult(argThat(result -> !result.isWinner()));
        verify(playEventRepository).save(argThat(playEntity -> !playEntity.isWinner()));
    }
    
    @Test
    void testProcessPlayEvent_WinAllocation_CreatesWinnerResult() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        GameEntity game = createGame("game-456", "campaign-123");
        GameBrandBudgetEntity budget = createBrandBudget("game-456", "brand-1", 100);
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(gameBrandBudgetRepository.findByGameId("game-456"))
            .thenReturn(Collections.singletonList(budget));
        when(budgetDecrementer.getCurrentBudget("game-456", "brand-1")).thenReturn(50);
        when(probabilityCalculator.calculateAllocation(anyInt(), any(), any(), anyInt()))
            .thenReturn(1); // Win
        when(budgetDecrementer.decrementBudget("game-456", "brand-1", 1))
            .thenReturn(true);
        
        rewardAllocationService.processPlayEvent(event);
        
        verify(budgetDecrementer).decrementBudget("game-456", "brand-1", 1);
        verify(redisResultStore).storeResult(argThat(result -> 
            result.isWinner() && result.getPlayId().equals("play-123")
        ));
        verify(playEventRepository).save(argThat(playEntity -> 
            playEntity.isWinner() && playEntity.getStatus() == PlayStatus.WINNER
        ));
    }
    
    @Test
    void testProcessPlayEvent_LoseAllocation_CreatesLoserResult() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        GameEntity game = createGame("game-456", "campaign-123");
        GameBrandBudgetEntity budget = createBrandBudget("game-456", "brand-1", 100);
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(gameBrandBudgetRepository.findByGameId("game-456"))
            .thenReturn(Collections.singletonList(budget));
        when(budgetDecrementer.getCurrentBudget("game-456", "brand-1")).thenReturn(50);
        when(probabilityCalculator.calculateAllocation(anyInt(), any(), any(), anyInt()))
            .thenReturn(0); // Lose
        
        rewardAllocationService.processPlayEvent(event);
        
        verify(budgetDecrementer, never()).decrementBudget(anyString(), anyString(), anyInt());
        verify(redisResultStore).storeResult(argThat(result -> !result.isWinner()));
        verify(playEventRepository).save(argThat(playEntity -> !playEntity.isWinner()));
    }
    
    @Test
    void testProcessPlayEvent_BudgetDecrementFails_CreatesLoserResult() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        GameEntity game = createGame("game-456", "campaign-123");
        GameBrandBudgetEntity budget = createBrandBudget("game-456", "brand-1", 100);
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(gameBrandBudgetRepository.findByGameId("game-456"))
            .thenReturn(Collections.singletonList(budget));
        when(budgetDecrementer.getCurrentBudget("game-456", "brand-1")).thenReturn(50);
        when(probabilityCalculator.calculateAllocation(anyInt(), any(), any(), anyInt()))
            .thenReturn(1);
        when(budgetDecrementer.decrementBudget("game-456", "brand-1", 1))
            .thenReturn(false); // Failed to decrement (race condition)
        
        rewardAllocationService.processPlayEvent(event);
        
        verify(redisResultStore).storeResult(argThat(result -> !result.isWinner()));
        verify(playEventRepository).save(argThat(playEntity -> !playEntity.isWinner()));
    }
    
    @Test
    void testProcessPlayEvent_MultipleBrands_SelectsRandomly() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        GameEntity game = createGame("game-456", "campaign-123");
        
        GameBrandBudgetEntity budget1 = createBrandBudget("game-456", "brand-1", 100);
        GameBrandBudgetEntity budget2 = createBrandBudget("game-456", "brand-2", 200);
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(gameBrandBudgetRepository.findByGameId("game-456"))
            .thenReturn(Arrays.asList(budget1, budget2));
        when(budgetDecrementer.getCurrentBudget("game-456", "brand-1")).thenReturn(50);
        when(budgetDecrementer.getCurrentBudget("game-456", "brand-2")).thenReturn(100);
        when(probabilityCalculator.calculateAllocation(eq(150), any(), any(), anyInt()))
            .thenReturn(1); // Total budget is 150
        when(budgetDecrementer.decrementBudget(anyString(), anyString(), eq(1)))
            .thenReturn(true);
        
        rewardAllocationService.processPlayEvent(event);
        
        // Should decrement from one of the brands
        verify(budgetDecrementer, times(1)).decrementBudget(
            eq("game-456"), 
            anyString(), 
            eq(1)
        );
        verify(redisResultStore).storeResult(argThat(result -> result.isWinner()));
    }
    
    @Test
    void testProcessPlayEvent_StoresCorrectPlayEntity() {
        PlayEventDto event = createPlayEvent("play-123", "game-456", "user-789");
        GameEntity game = createGame("game-456", "campaign-123");
        GameBrandBudgetEntity budget = createBrandBudget("game-456", "brand-1", 100);
        
        when(idempotencyHandler.tryMarkAsProcessed("play-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(gameBrandBudgetRepository.findByGameId("game-456"))
            .thenReturn(Collections.singletonList(budget));
        when(budgetDecrementer.getCurrentBudget("game-456", "brand-1")).thenReturn(50);
        when(probabilityCalculator.calculateAllocation(anyInt(), any(), any(), anyInt()))
            .thenReturn(1);
        when(budgetDecrementer.decrementBudget("game-456", "brand-1", 1))
            .thenReturn(true);
        
        rewardAllocationService.processPlayEvent(event);
        
        ArgumentCaptor<PlayEventEntity> captor = ArgumentCaptor.forClass(PlayEventEntity.class);
        verify(playEventRepository).save(captor.capture());
        
        PlayEventEntity savedEntity = captor.getValue();
        assertEquals("play-123", savedEntity.getId());
        assertEquals("game-456", savedEntity.getGameId());
        assertEquals("user-789", savedEntity.getUserId());
        assertTrue(savedEntity.isWinner());
        assertEquals(PlayStatus.WINNER, savedEntity.getStatus());
        assertNotNull(savedEntity.getCouponId());
    }
    
    private PlayEventDto createPlayEvent(String playId, String gameId, String userId) {
        return PlayEventDto.builder()
            .playId(playId)
            .gameId(gameId)
            .userId(userId)
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private GameEntity createGame(String gameId, String campaignId) {
        GameEntity game = new GameEntity();
        game.setId(gameId);
        game.setCampaignId(campaignId);
        game.setStartTime(LocalDateTime.now().minusHours(1));
        game.setEndTime(LocalDateTime.now().plusHours(1));
        game.setSlotGranularitySeconds(60);
        return game;
    }
    
    private GameBrandBudgetEntity createBrandBudget(String gameId, String brandId, int budget) {
        GameBrandBudgetEntity entity = new GameBrandBudgetEntity();
        entity.setGameId(gameId);
        entity.setBrandId(brandId);
        entity.setTotalBudget(budget);
        return entity;
    }
}
