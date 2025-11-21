package com.frolic.core.cache.store;

import com.frolic.core.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis store for budget management
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisBudgetStore {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * Initialize budget in Redis for a game-brand combination
     */
    public void initializeBudget(String gameId, String brandId, int budget) {
        String key = RedisKeys.budgetKey(gameId, brandId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(budget));
        log.info("Initialized budget for game={}, brand={}, amount={}", gameId, brandId, budget);
    }
    
    /**
     * Get remaining budget for a game-brand combination
     */
    public Integer getRemainingBudget(String gameId, String brandId) {
        String key = RedisKeys.budgetKey(gameId, brandId);
        String value = stringRedisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }
    
    /**
     * Decrement budget by specified amount
     */
    public boolean decrementBudget(String gameId, String brandId, int amount) {
        String key = RedisKeys.budgetKey(gameId, brandId);
        Long result = stringRedisTemplate.opsForValue().decrement(key, amount);
        return result != null && result >= 0;
    }
    
    /**
     * Clear budget for a game-brand combination
     */
    public void clearBudget(String gameId, String brandId) {
        String key = RedisKeys.budgetKey(gameId, brandId);
        stringRedisTemplate.delete(key);
        log.info("Cleared budget for game={}, brand={}", gameId, brandId);
    }
    
    /**
     * Set expiration for budget key
     */
    public void expireBudget(String gameId, String brandId, long seconds) {
        String key = RedisKeys.budgetKey(gameId, brandId);
        stringRedisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }
}
