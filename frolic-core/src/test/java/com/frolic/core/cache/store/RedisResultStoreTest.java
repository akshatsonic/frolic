package com.frolic.core.cache.store;

import com.frolic.core.common.constant.RedisKeys;
import com.frolic.core.common.dto.PlayResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisResultStore
 */
@ExtendWith(MockitoExtension.class)
class RedisResultStoreTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    private RedisResultStore redisResultStore;
    
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisResultStore = new RedisResultStore(redisTemplate);
    }
    
    @Test
    void testStoreResult_SavesResultWithTTL() {
        PlayResultDto result = PlayResultDto.builder()
            .playId("play-123")
            .gameId("game-456")
            .userId("user-789")
            .winner(true)
            .couponId("coupon-abc")
            .brandId("brand-xyz")
            .brandName("Brand X")
            .timestamp(LocalDateTime.now())
            .message("Congratulations!")
            .build();
        
        String key = RedisKeys.resultKey(result.getPlayId());
        
        redisResultStore.storeResult(result);
        
        verify(valueOperations).set(eq(key), eq(result), eq(3600L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    void testStoreResult_LoserResult() {
        PlayResultDto result = PlayResultDto.builder()
            .playId("play-456")
            .gameId("game-789")
            .userId("user-101")
            .winner(false)
            .timestamp(LocalDateTime.now())
            .message("Better luck next time!")
            .build();
        
        String key = RedisKeys.resultKey(result.getPlayId());
        
        redisResultStore.storeResult(result);
        
        verify(valueOperations).set(eq(key), eq(result), eq(3600L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    void testGetResult_WhenExists_ReturnsResult() {
        String playId = "play-789";
        String key = RedisKeys.resultKey(playId);
        
        PlayResultDto expected = PlayResultDto.builder()
            .playId(playId)
            .gameId("game-123")
            .userId("user-456")
            .winner(true)
            .couponId("coupon-def")
            .timestamp(LocalDateTime.now())
            .build();
        
        when(valueOperations.get(key)).thenReturn(expected);
        
        PlayResultDto result = redisResultStore.getResult(playId);
        
        assertNotNull(result);
        assertEquals(playId, result.getPlayId());
        assertEquals("game-123", result.getGameId());
        assertEquals("user-456", result.getUserId());
        assertTrue(result.isWinner());
        verify(valueOperations).get(key);
    }
    
    @Test
    void testGetResult_WhenNotExists_ReturnsNull() {
        String playId = "play-nonexistent";
        String key = RedisKeys.resultKey(playId);
        
        when(valueOperations.get(key)).thenReturn(null);
        
        PlayResultDto result = redisResultStore.getResult(playId);
        
        assertNull(result);
        verify(valueOperations).get(key);
    }
    
    @Test
    void testResultExists_WhenExists_ReturnsTrue() {
        String playId = "play-123";
        String key = RedisKeys.resultKey(playId);
        
        when(redisTemplate.hasKey(key)).thenReturn(true);
        
        boolean exists = redisResultStore.resultExists(playId);
        
        assertTrue(exists);
        verify(redisTemplate).hasKey(key);
    }
    
    @Test
    void testResultExists_WhenNotExists_ReturnsFalse() {
        String playId = "play-456";
        String key = RedisKeys.resultKey(playId);
        
        when(redisTemplate.hasKey(key)).thenReturn(false);
        
        boolean exists = redisResultStore.resultExists(playId);
        
        assertFalse(exists);
        verify(redisTemplate).hasKey(key);
    }
    
    @Test
    void testResultExists_WhenNull_ReturnsFalse() {
        String playId = "play-789";
        String key = RedisKeys.resultKey(playId);
        
        when(redisTemplate.hasKey(key)).thenReturn(null);
        
        boolean exists = redisResultStore.resultExists(playId);
        
        assertFalse(exists);
    }
    
    @Test
    void testDeleteResult_RemovesFromRedis() {
        String playId = "play-delete";
        String key = RedisKeys.resultKey(playId);
        
        redisResultStore.deleteResult(playId);
        
        verify(redisTemplate).delete(key);
    }
    
    @Test
    void testStoreAndRetrieve_RoundTrip() {
        PlayResultDto original = PlayResultDto.builder()
            .playId("play-roundtrip")
            .gameId("game-rt")
            .userId("user-rt")
            .winner(true)
            .couponId("coupon-rt")
            .brandId("brand-rt")
            .brandName("Test Brand")
            .timestamp(LocalDateTime.now())
            .message("Test message")
            .build();
        
        String key = RedisKeys.resultKey(original.getPlayId());
        
        // Simulate store and retrieve
        when(valueOperations.get(key)).thenReturn(original);
        
        redisResultStore.storeResult(original);
        PlayResultDto retrieved = redisResultStore.getResult(original.getPlayId());
        
        assertNotNull(retrieved);
        assertEquals(original.getPlayId(), retrieved.getPlayId());
        assertEquals(original.isWinner(), retrieved.isWinner());
    }
    
    @Test
    void testMultipleResults_IndependentStorage() {
        PlayResultDto result1 = PlayResultDto.builder()
            .playId("play-1")
            .gameId("game-1")
            .userId("user-1")
            .winner(true)
            .timestamp(LocalDateTime.now())
            .build();
        
        PlayResultDto result2 = PlayResultDto.builder()
            .playId("play-2")
            .gameId("game-2")
            .userId("user-2")
            .winner(false)
            .timestamp(LocalDateTime.now())
            .build();
        
        redisResultStore.storeResult(result1);
        redisResultStore.storeResult(result2);
        
        verify(valueOperations).set(eq(RedisKeys.resultKey("play-1")), 
            eq(result1), anyLong(), any(TimeUnit.class));
        verify(valueOperations).set(eq(RedisKeys.resultKey("play-2")), 
            eq(result2), anyLong(), any(TimeUnit.class));
    }
}
