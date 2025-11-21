package com.frolic.core.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RedisKeys constant class
 */
class RedisKeysTest {
    
    @Test
    void testBudgetKey_GeneratesCorrectPattern() {
        String gameId = "game-123";
        String brandId = "brand-456";
        
        String key = RedisKeys.budgetKey(gameId, brandId);
        
        assertEquals("budget:game:game-123:brand:brand-456", key);
    }
    
    @Test
    void testBudgetKey_WithDifferentIds() {
        String key1 = RedisKeys.budgetKey("game-1", "brand-A");
        String key2 = RedisKeys.budgetKey("game-2", "brand-B");
        
        assertNotEquals(key1, key2);
        assertTrue(key1.contains("game-1"));
        assertTrue(key1.contains("brand-A"));
        assertTrue(key2.contains("game-2"));
        assertTrue(key2.contains("brand-B"));
    }
    
    @Test
    void testResultKey_GeneratesCorrectPattern() {
        String playId = "play-789";
        
        String key = RedisKeys.resultKey(playId);
        
        assertEquals("result:play-789", key);
    }
    
    @Test
    void testResultKey_WithDifferentPlayIds() {
        String key1 = RedisKeys.resultKey("play-1");
        String key2 = RedisKeys.resultKey("play-2");
        
        assertNotEquals(key1, key2);
        assertEquals("result:play-1", key1);
        assertEquals("result:play-2", key2);
    }
    
    @Test
    void testSlotsKey_GeneratesCorrectPattern() {
        String gameId = "game-555";
        
        String key = RedisKeys.slotsKey(gameId);
        
        assertEquals("slots:game:game-555", key);
    }
    
    @Test
    void testIdempotencyKey_GeneratesCorrectPattern() {
        String playId = "play-abc123";
        
        String key = RedisKeys.idempotencyKey(playId);
        
        assertEquals("play_processed:play-abc123", key);
    }
    
    @Test
    void testGameConfigKey_GeneratesCorrectPattern() {
        String gameId = "game-config-1";
        
        String key = RedisKeys.gameConfigKey(gameId);
        
        assertEquals("game_config:game-config-1", key);
    }
    
    @Test
    void testAllKeys_Unique() {
        String gameId = "game-1";
        String brandId = "brand-1";
        String playId = "play-1";
        
        String budgetKey = RedisKeys.budgetKey(gameId, brandId);
        String resultKey = RedisKeys.resultKey(playId);
        String slotsKey = RedisKeys.slotsKey(gameId);
        String idempotencyKey = RedisKeys.idempotencyKey(playId);
        String gameConfigKey = RedisKeys.gameConfigKey(gameId);
        
        // All keys should be unique
        assertNotEquals(budgetKey, resultKey);
        assertNotEquals(budgetKey, slotsKey);
        assertNotEquals(resultKey, slotsKey);
        assertNotEquals(idempotencyKey, resultKey);
        assertNotEquals(gameConfigKey, budgetKey);
    }
    
    @Test
    void testBudgetKey_WithEmptyStrings() {
        String key = RedisKeys.budgetKey("", "");
        
        assertEquals("budget:game::brand:", key);
    }
    
    @Test
    void testKeyPatterns_AreCorrect() {
        assertEquals("budget:game:%s:brand:%s", RedisKeys.BUDGET_KEY_PATTERN);
        assertEquals("result:%s", RedisKeys.RESULT_KEY_PATTERN);
        assertEquals("slots:game:%s", RedisKeys.SLOTS_KEY_PATTERN);
        assertEquals("play_processed:%s", RedisKeys.IDEMPOTENCY_KEY_PATTERN);
        assertEquals("game_config:%s", RedisKeys.GAME_CONFIG_KEY_PATTERN);
    }
}
