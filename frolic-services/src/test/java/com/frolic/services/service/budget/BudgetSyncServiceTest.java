package com.frolic.services.service.budget;

import com.frolic.core.cache.store.RedisBudgetStore;
import com.frolic.core.repository.entity.GameBrandBudgetEntity;
import com.frolic.core.repository.jpa.GameBrandBudgetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BudgetSyncService
 */
@ExtendWith(MockitoExtension.class)
class BudgetSyncServiceTest {
    
    @Mock
    private RedisBudgetStore redisBudgetStore;
    
    @Mock
    private GameBrandBudgetRepository budgetRepository;
    
    @InjectMocks
    private BudgetSyncService budgetSyncService;
    
    @Test
    void testSyncBudgetsFromRedisToPostgres_ValidGameId_SyncsBudgets() {
        String gameId = "game-1";
        
        GameBrandBudgetEntity budget1 = createBudgetEntity(gameId, "brand-1", 100, 0, 100);
        GameBrandBudgetEntity budget2 = createBudgetEntity(gameId, "brand-2", 200, 0, 200);
        List<GameBrandBudgetEntity> budgets = Arrays.asList(budget1, budget2);
        
        when(budgetRepository.findByGameId(gameId)).thenReturn(budgets);
        when(redisBudgetStore.getRemainingBudget(gameId, "brand-1")).thenReturn(40);
        when(redisBudgetStore.getRemainingBudget(gameId, "brand-2")).thenReturn(150);
        when(budgetRepository.save(any(GameBrandBudgetEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        budgetSyncService.syncBudgetsFromRedisToPostgres(gameId);
        
        verify(budgetRepository).findByGameId(gameId);
        verify(redisBudgetStore).getRemainingBudget(gameId, "brand-1");
        verify(redisBudgetStore).getRemainingBudget(gameId, "brand-2");
        verify(budgetRepository, times(2)).save(any(GameBrandBudgetEntity.class));
        
        // Verify budget1 was updated correctly (100 total - 40 remaining = 60 allocated)
        assertThat(budget1.getAllocatedBudget()).isEqualTo(60);
        assertThat(budget1.getRemainingBudget()).isEqualTo(40);
        
        // Verify budget2 was updated correctly (200 total - 150 remaining = 50 allocated)
        assertThat(budget2.getAllocatedBudget()).isEqualTo(50);
        assertThat(budget2.getRemainingBudget()).isEqualTo(150);
    }
    
    @Test
    void testSyncBudgetsFromRedisToPostgres_NoBudgetsInRedis_SkipsSync() {
        String gameId = "game-1";
        
        GameBrandBudgetEntity budget = createBudgetEntity(gameId, "brand-1", 100, 0, 100);
        
        when(budgetRepository.findByGameId(gameId)).thenReturn(Arrays.asList(budget));
        when(redisBudgetStore.getRemainingBudget(gameId, "brand-1")).thenReturn(null);
        
        budgetSyncService.syncBudgetsFromRedisToPostgres(gameId);
        
        verify(budgetRepository).findByGameId(gameId);
        verify(redisBudgetStore).getRemainingBudget(gameId, "brand-1");
        verify(budgetRepository, never()).save(any(GameBrandBudgetEntity.class));
    }
    
    @Test
    void testSyncBudgetsFromRedisToPostgres_EmptyBudgetList_DoesNothing() {
        String gameId = "game-1";
        
        when(budgetRepository.findByGameId(gameId)).thenReturn(Arrays.asList());
        
        budgetSyncService.syncBudgetsFromRedisToPostgres(gameId);
        
        verify(budgetRepository).findByGameId(gameId);
        verify(redisBudgetStore, never()).getRemainingBudget(anyString(), anyString());
        verify(budgetRepository, never()).save(any(GameBrandBudgetEntity.class));
    }
    
    @Test
    void testSyncBudgetsFromRedisToPostgres_ExceptionDuringSyncOneBudget_ContinuesWithOthers() {
        String gameId = "game-1";
        
        GameBrandBudgetEntity budget1 = createBudgetEntity(gameId, "brand-1", 100, 0, 100);
        GameBrandBudgetEntity budget2 = createBudgetEntity(gameId, "brand-2", 200, 0, 200);
        List<GameBrandBudgetEntity> budgets = Arrays.asList(budget1, budget2);
        
        when(budgetRepository.findByGameId(gameId)).thenReturn(budgets);
        when(redisBudgetStore.getRemainingBudget(gameId, "brand-1"))
            .thenThrow(new RuntimeException("Redis error"));
        when(redisBudgetStore.getRemainingBudget(gameId, "brand-2")).thenReturn(150);
        when(budgetRepository.save(any(GameBrandBudgetEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Should not throw exception, just log and continue
        assertThatCode(() -> budgetSyncService.syncBudgetsFromRedisToPostgres(gameId))
            .doesNotThrowAnyException();
        
        verify(budgetRepository).findByGameId(gameId);
        verify(redisBudgetStore).getRemainingBudget(gameId, "brand-1");
        verify(redisBudgetStore).getRemainingBudget(gameId, "brand-2");
        verify(budgetRepository, times(1)).save(budget2); // Only budget2 should be saved
    }
    
    @Test
    void testSyncBrandBudget_ValidGameIdAndBrandId_SyncsBudget() {
        String gameId = "game-1";
        String brandId = "brand-1";
        
        GameBrandBudgetEntity budget = createBudgetEntity(gameId, brandId, 100, 0, 100);
        
        when(budgetRepository.findByGameIdAndBrandId(gameId, brandId))
            .thenReturn(Optional.of(budget));
        when(redisBudgetStore.getRemainingBudget(gameId, brandId)).thenReturn(30);
        when(budgetRepository.save(any(GameBrandBudgetEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        budgetSyncService.syncBrandBudget(gameId, brandId);
        
        verify(budgetRepository).findByGameIdAndBrandId(gameId, brandId);
        verify(redisBudgetStore).getRemainingBudget(gameId, brandId);
        verify(budgetRepository).save(budget);
        
        assertThat(budget.getAllocatedBudget()).isEqualTo(70); // 100 - 30
        assertThat(budget.getRemainingBudget()).isEqualTo(30);
    }
    
    @Test
    void testSyncBrandBudget_BudgetNotFound_ThrowsException() {
        String gameId = "game-1";
        String brandId = "brand-1";
        
        when(budgetRepository.findByGameIdAndBrandId(gameId, brandId))
            .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> budgetSyncService.syncBrandBudget(gameId, brandId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Budget not found");
        
        verify(budgetRepository).findByGameIdAndBrandId(gameId, brandId);
        verify(redisBudgetStore, never()).getRemainingBudget(anyString(), anyString());
        verify(budgetRepository, never()).save(any(GameBrandBudgetEntity.class));
    }
    
    @Test
    void testSyncBrandBudget_NoBudgetInRedis_DoesNotSave() {
        String gameId = "game-1";
        String brandId = "brand-1";
        
        GameBrandBudgetEntity budget = createBudgetEntity(gameId, brandId, 100, 0, 100);
        
        when(budgetRepository.findByGameIdAndBrandId(gameId, brandId))
            .thenReturn(Optional.of(budget));
        when(redisBudgetStore.getRemainingBudget(gameId, brandId)).thenReturn(null);
        
        budgetSyncService.syncBrandBudget(gameId, brandId);
        
        verify(budgetRepository).findByGameIdAndBrandId(gameId, brandId);
        verify(redisBudgetStore).getRemainingBudget(gameId, brandId);
        verify(budgetRepository, never()).save(any(GameBrandBudgetEntity.class));
    }
    
    private GameBrandBudgetEntity createBudgetEntity(String gameId, String brandId, 
                                                      int totalBudget, int allocatedBudget, int remainingBudget) {
        GameBrandBudgetEntity entity = new GameBrandBudgetEntity();
        entity.setId("budget-" + brandId);
        entity.setGameId(gameId);
        entity.setBrandId(brandId);
        entity.setTotalBudget(totalBudget);
        entity.setAllocatedBudget(allocatedBudget);
        entity.setRemainingBudget(remainingBudget);
        return entity;
    }
}
