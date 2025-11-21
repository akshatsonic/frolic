package com.frolic.core.engine.concurrency;

import com.frolic.core.common.constant.RedisKeys;
import com.frolic.core.common.exception.ConcurrencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AtomicBudgetDecrementer
 */
@ExtendWith(MockitoExtension.class)
class AtomicBudgetDecrementerTest {
    
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    
    @Mock
    private RedisScript<Long> budgetDecrementScript;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    private AtomicBudgetDecrementer budgetDecrementer;
    
    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        budgetDecrementer = new AtomicBudgetDecrementer(stringRedisTemplate, budgetDecrementScript);
    }
    
    @Test
    void testDecrementBudget_Success_ReturnsTrue() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 5;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(stringRedisTemplate.execute(
            eq(budgetDecrementScript),
            eq(Collections.singletonList(key)),
            eq(String.valueOf(amount))
        )).thenReturn(10L); // Remaining budget after decrement
        
        boolean result = budgetDecrementer.decrementBudget(gameId, brandId, amount);
        
        assertTrue(result);
        verify(stringRedisTemplate).execute(
            eq(budgetDecrementScript),
            eq(Collections.singletonList(key)),
            eq(String.valueOf(amount))
        );
    }
    
    @Test
    void testDecrementBudget_InsufficientBudget_ReturnsFalse() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 10;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(stringRedisTemplate.execute(
            eq(budgetDecrementScript),
            eq(Collections.singletonList(key)),
            eq(String.valueOf(amount))
        )).thenReturn(-1L); // Negative indicates insufficient budget
        
        boolean result = budgetDecrementer.decrementBudget(gameId, brandId, amount);
        
        assertFalse(result);
    }
    
    @Test
    void testDecrementBudget_NullResult_ReturnsFalse() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 5;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(stringRedisTemplate.execute(
            eq(budgetDecrementScript),
            eq(Collections.singletonList(key)),
            eq(String.valueOf(amount))
        )).thenReturn(null);
        
        boolean result = budgetDecrementer.decrementBudget(gameId, brandId, amount);
        
        assertFalse(result);
    }
    
    @Test
    void testDecrementBudget_ZeroRemaining_ReturnsTrue() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 10;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(stringRedisTemplate.execute(
            eq(budgetDecrementScript),
            eq(Collections.singletonList(key)),
            eq(String.valueOf(amount))
        )).thenReturn(0L); // Exactly zero remaining
        
        boolean result = budgetDecrementer.decrementBudget(gameId, brandId, amount);
        
        assertTrue(result);
    }
    
    @Test
    void testDecrementBudget_ExceptionThrown_ThrowsConcurrencyException() {
        String gameId = "game-123";
        String brandId = "brand-456";
        int amount = 5;
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(stringRedisTemplate.execute(
            eq(budgetDecrementScript),
            eq(Collections.singletonList(key)),
            eq(String.valueOf(amount))
        )).thenThrow(new RuntimeException("Redis error"));
        
        assertThrows(ConcurrencyException.class, () -> 
            budgetDecrementer.decrementBudget(gameId, brandId, amount)
        );
    }
    
    @Test
    void testGetCurrentBudget_ReturnsCorrectValue() {
        String gameId = "game-789";
        String brandId = "brand-101";
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.get(key)).thenReturn("100");
        
        int budget = budgetDecrementer.getCurrentBudget(gameId, brandId);
        
        assertEquals(100, budget);
        verify(valueOperations).get(key);
    }
    
    @Test
    void testGetCurrentBudget_NullValue_ReturnsZero() {
        String gameId = "game-789";
        String brandId = "brand-101";
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.get(key)).thenReturn(null);
        
        int budget = budgetDecrementer.getCurrentBudget(gameId, brandId);
        
        assertEquals(0, budget);
    }
    
    @Test
    void testGetCurrentBudget_ZeroValue_ReturnsZero() {
        String gameId = "game-789";
        String brandId = "brand-101";
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(valueOperations.get(key)).thenReturn("0");
        
        int budget = budgetDecrementer.getCurrentBudget(gameId, brandId);
        
        assertEquals(0, budget);
    }
    
    @Test
    void testDecrementBudget_MultipleInvocations() {
        String gameId = "game-123";
        String brandId = "brand-456";
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        when(stringRedisTemplate.execute(
            eq(budgetDecrementScript),
            eq(Collections.singletonList(key)),
            anyString()
        )).thenReturn(50L, 40L, 30L);
        
        boolean result1 = budgetDecrementer.decrementBudget(gameId, brandId, 10);
        boolean result2 = budgetDecrementer.decrementBudget(gameId, brandId, 10);
        boolean result3 = budgetDecrementer.decrementBudget(gameId, brandId, 10);
        
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        verify(stringRedisTemplate, times(3)).execute(
            eq(budgetDecrementScript),
            anyList(),
            anyString()
        );
    }
}
