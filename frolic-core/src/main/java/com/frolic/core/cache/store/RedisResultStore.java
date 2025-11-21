package com.frolic.core.cache.store;

import com.frolic.core.common.constant.RedisKeys;
import com.frolic.core.common.dto.PlayResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis store for play results
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisResultStore {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final long RESULT_TTL_SECONDS = 3600; // 1 hour
    
    /**
     * Store play result in Redis
     */
    public void storeResult(PlayResultDto result) {
        String key = RedisKeys.resultKey(result.getPlayId());
        redisTemplate.opsForValue().set(key, result, RESULT_TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("Stored result for playId={}", result.getPlayId());
    }
    
    /**
     * Retrieve play result from Redis
     */
    public PlayResultDto getResult(String playId) {
        String key = RedisKeys.resultKey(playId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? (PlayResultDto) value : null;
    }
    
    /**
     * Check if result exists
     */
    public boolean resultExists(String playId) {
        String key = RedisKeys.resultKey(playId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Delete result from Redis
     */
    public void deleteResult(String playId) {
        String key = RedisKeys.resultKey(playId);
        redisTemplate.delete(key);
    }
}
