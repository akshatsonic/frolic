package com.frolic.core.engine.concurrency;

import com.frolic.core.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Idempotency handler to prevent duplicate processing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyHandler {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    
    /**
     * Check if play has already been processed
     */
    public boolean isAlreadyProcessed(String playId) {
        String key = RedisKeys.idempotencyKey(playId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Mark play as processed
     */
    public void markAsProcessed(String playId) {
        String key = RedisKeys.idempotencyKey(playId);
        redisTemplate.opsForValue().set(key, true, IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Marked play as processed: playId={}", playId);
    }
    
    /**
     * Try to mark as processed atomically (set if not exists)
     * Returns true if successfully set (not processed before), false otherwise
     */
    public boolean tryMarkAsProcessed(String playId) {
        String key = RedisKeys.idempotencyKey(playId);
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(key, true, IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
        
        boolean success = Boolean.TRUE.equals(result);
        if (success) {
            log.debug("Successfully marked play as processed: playId={}", playId);
        } else {
            log.debug("Play already processed: playId={}", playId);
        }
        
        return success;
    }
}
