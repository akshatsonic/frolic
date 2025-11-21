package com.frolic.core.cache.store;

import com.frolic.core.common.constant.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisBudgetStore
 */
@ExtendWith(MockitoExtension.class)
class RedisBudgetStoreTest {
    
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    private RedisBudgetStore redisBudgetStore;
    
    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        redisBudgetStore = new RedisBudgetStore(stringRedisTemplate);
    }
    
    @Test
    void testInitializeBudget_SetsBudgetInRedis() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int budget = 100;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        redisBudgetStore.initializeBudget(gameId, brandId, budget);
        
        verify(valueOperations).set(key, "100");
    }
    
    @Test
    void testInitializeBudget_WithZeroBudget() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int budget = 0;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        redisBudgetStore.initializeBudget(gameId, brandId, budget);
        
        verify(valueOperations).set(key, "0");
    }
    
    @Test
    void testGetRemainingBudget_ReturnsCorrectValue() {
        String gameId = "game-789";
        String brandId = "brand-101";
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.get(key)).thenReturn("250");
        
        Integer remaining = redisBudgetStore.getRemainingBudget(gameId, brandId);
        
        assertEquals(250, remaining);
        verify(valueOperations).get(key);
    }
    
    @Test
    void testGetRemainingBudget_WhenKeyDoesNotExist_ReturnsZero() {
        String gameId = "game-789";
        String brandId = "brand-101";
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.get(key)).thenReturn(null);
        
        Integer remaining = redisBudgetStore.getRemainingBudget(gameId, brandId);
        
        assertEquals(0, remaining);
    }
    
    @Test
    void testDecrementBudget_Success_ReturnsTrue() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 10;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.decrement(key, amount)).thenReturn(40L);
        
        boolean result = redisBudgetStore.decrementBudget(gameId, brandId, amount);
        
        assertTrue(result);
        verify(valueOperations).decrement(key, amount);
    }
    
    @Test
    void testDecrementBudget_ResultingInZero_ReturnsTrue() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 50;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.decrement(key, amount)).thenReturn(0L);
        
        boolean result = redisBudgetStore.decrementBudget(gameId, brandId, amount);
        
        assertTrue(result);
    }
    
    @Test
    void testDecrementBudget_ResultingInNegative_ReturnsFalse() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 100;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.decrement(key, amount)).thenReturn(-10L);
        
        boolean result = redisBudgetStore.decrementBudget(gameId, brandId, amount);
        
        assertFalse(result);
    }
    
    @Test
    void testDecrementBudget_NullResult_ReturnsFalse() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 10;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.decrement(key, amount)).thenReturn(null);
        
        boolean result = redisBudgetStore.decrementBudget(gameId, brandId, amount);
        
        assertFalse(result);
    }
    
    @Test
    void testClearBudget_DeletesKey() {
        String gameId = "game-456";
        String brandId = "brand-789";
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        redisBudgetStore.clearBudget(gameId, brandId);
        
        verify(stringRedisTemplate).delete(key);
    }
    
    @Test
    void testExpireBudget_SetsExpiration() {
        String gameId = "game-111";
        String brandId = "brand-222";
        long seconds = 3600;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        redisBudgetStore.expireBudget(gameId, brandId, seconds);
        
        verify(stringRedisTemplate).expire(key, seconds, TimeUnit.SECONDS);
    }
    
    @Test
    void testMultipleBrands_IndependentBudgets() {
        String gameId = "game-999";
        String brand1 = "brand-A";
        String brand2 = "brand-B";
        
        redisBudgetStore.initializeBudget(gameId, brand1, 100);
        redisBudgetStore.initializeBudget(gameId, brand2, 200);
        
        verify(valueOperations).set(RedisKeys.budgetKey(gameId, brand1), "100");
        verify(valueOperations).set(RedisKeys.budgetKey(gameId, brand2), "200");
    }
}
