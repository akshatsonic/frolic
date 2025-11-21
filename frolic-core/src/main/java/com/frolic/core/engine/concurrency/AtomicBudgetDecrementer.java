package com.frolic.core.engine.concurrency;

import com.frolic.core.common.constant.RedisKeys;
import com.frolic.core.common.exception.ConcurrencyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Atomic budget decrement using Redis Lua scripts
 * Ensures no race conditions in budget allocation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AtomicBudgetDecrementer {
    
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> budgetDecrementScript;
    
    /**
     * Atomically decrement budget for a game-brand combination
     * 
     * @param gameId Game ID
     * @param brandId Brand ID
     * @param amount Amount to decrement
     * @return true if decrement was successful, false if insufficient budget
     */
    public boolean decrementBudget(String gameId, String brandId, int amount) {
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        try {
            Long result = stringRedisTemplate.execute(
                budgetDecrementScript,
                Collections.singletonList(key),
                String.valueOf(amount)
            );
            
            if (result == null) {
                log.error("Null result from budget decrement script for game={}, brand={}", gameId, brandId);
                return false;
            }
            
            if (result >= 0) {
                log.debug("Successfully decremented budget for game={}, brand={}, remaining={}", 
                    gameId, brandId, result);
                return true;
            } else {
                log.debug("Insufficient budget for game={}, brand={}", gameId, brandId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error executing budget decrement script for game={}, brand={}", 
                gameId, brandId, e);
            throw new ConcurrencyException("Failed to decrement budget atomically", e);
        }
    }
    
    /**
     * Get current budget without decrementing
     */
    public int getCurrentBudget(String gameId, String brandId) {
        String key = RedisKeys.budgetKey(gameId, brandId);
        String value = stringRedisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }
}
