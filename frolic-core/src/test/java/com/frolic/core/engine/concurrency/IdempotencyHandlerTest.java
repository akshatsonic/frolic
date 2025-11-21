package com.frolic.core.engine.concurrency;

import com.frolic.core.common.constant.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdempotencyHandler
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyHandlerTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    private IdempotencyHandler idempotencyHandler;
    
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyHandler = new IdempotencyHandler(redisTemplate);
    }
    
    @Test
    void testIsAlreadyProcessed_WhenKeyExists_ReturnsTrue() {
        String playId = "play-123";
        String expectedKey = RedisKeys.idempotencyKey(playId);
        
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);
        
        boolean result = idempotencyHandler.isAlreadyProcessed(playId);
        
        assertTrue(result);
        verify(redisTemplate).hasKey(expectedKey);
    }
    
    @Test
    void testIsAlreadyProcessed_WhenKeyDoesNotExist_ReturnsFalse() {
        String playId = "play-123";
        String expectedKey = RedisKeys.idempotencyKey(playId);
        
        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);
        
        boolean result = idempotencyHandler.isAlreadyProcessed(playId);
        
        assertFalse(result);
        verify(redisTemplate).hasKey(expectedKey);
    }
    
    @Test
    void testIsAlreadyProcessed_WhenKeyIsNull_ReturnsFalse() {
        String playId = "play-123";
        String expectedKey = RedisKeys.idempotencyKey(playId);
        
        when(redisTemplate.hasKey(expectedKey)).thenReturn(null);
        
        boolean result = idempotencyHandler.isAlreadyProcessed(playId);
        
        assertFalse(result);
    }
    
    @Test
    void testMarkAsProcessed_SetsKeyWithTTL() {
        String playId = "play-456";
        String expectedKey = RedisKeys.idempotencyKey(playId);
        
        idempotencyHandler.markAsProcessed(playId);
        
        verify(valueOperations).set(eq(expectedKey), eq(true), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void testTryMarkAsProcessed_WhenNotProcessed_ReturnsTrueAndSetsKey() {
        String playId = "play-789";
        String expectedKey = RedisKeys.idempotencyKey(playId);
        
        when(valueOperations.setIfAbsent(eq(expectedKey), eq(true), eq(24L), eq(TimeUnit.HOURS)))
            .thenReturn(true);
        
        boolean result = idempotencyHandler.tryMarkAsProcessed(playId);
        
        assertTrue(result);
        verify(valueOperations).setIfAbsent(eq(expectedKey), eq(true), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void testTryMarkAsProcessed_WhenAlreadyProcessed_ReturnsFalse() {
        String playId = "play-789";
        String expectedKey = RedisKeys.idempotencyKey(playId);
        
        when(valueOperations.setIfAbsent(eq(expectedKey), eq(true), eq(24L), eq(TimeUnit.HOURS)))
            .thenReturn(false);
        
        boolean result = idempotencyHandler.tryMarkAsProcessed(playId);
        
        assertFalse(result);
        verify(valueOperations).setIfAbsent(eq(expectedKey), eq(true), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void testTryMarkAsProcessed_WhenSetIfAbsentReturnsNull_ReturnsFalse() {
        String playId = "play-999";
        String expectedKey = RedisKeys.idempotencyKey(playId);
        
        when(valueOperations.setIfAbsent(eq(expectedKey), eq(true), eq(24L), eq(TimeUnit.HOURS)))
            .thenReturn(null);
        
        boolean result = idempotencyHandler.tryMarkAsProcessed(playId);
        
        assertFalse(result);
    }
    
    @Test
    void testMarkAsProcessed_WithDifferentPlayIds() {
        String playId1 = "play-001";
        String playId2 = "play-002";
        
        idempotencyHandler.markAsProcessed(playId1);
        idempotencyHandler.markAsProcessed(playId2);
        
        verify(valueOperations).set(eq(RedisKeys.idempotencyKey(playId1)), 
            eq(true), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq(RedisKeys.idempotencyKey(playId2)), 
            eq(true), eq(24L), eq(TimeUnit.HOURS));
    }
}
