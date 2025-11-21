package com.frolic.core.common.constant;

/**
 * Redis key patterns used across the application
 */
public final class RedisKeys {
    
    private RedisKeys() {
        // Utility class
    }
    
    /**
     * Budget key pattern: budget:game:{gameId}:brand:{brandId}
     */
    public static final String BUDGET_KEY_PATTERN = "budget:game:%s:brand:%s";
    
    /**
     * Result key pattern: result:{playId}
     */
    public static final String RESULT_KEY_PATTERN = "result:%s";
    
    /**
     * Slots cache key pattern: slots:game:{gameId}
     */
    public static final String SLOTS_KEY_PATTERN = "slots:game:%s";
    
    /**
     * Idempotency key pattern: play_processed:{playId}
     */
    public static final String IDEMPOTENCY_KEY_PATTERN = "play_processed:%s";
    
    /**
     * Game configuration cache key pattern: game_config:{gameId}
     */
    public static final String GAME_CONFIG_KEY_PATTERN = "game_config:%s";
    
    /**
     * Generate budget key for a game and brand
     */
    public static String budgetKey(String gameId, String brandId) {
        return String.format(BUDGET_KEY_PATTERN, gameId, brandId);
    }
    
    /**
     * Generate result key for a play
     */
    public static String resultKey(String playId) {
        return String.format(RESULT_KEY_PATTERN, playId);
    }
    
    /**
     * Generate slots cache key for a game
     */
    public static String slotsKey(String gameId) {
        return String.format(SLOTS_KEY_PATTERN, gameId);
    }
    
    /**
     * Generate idempotency key for a play
     */
    public static String idempotencyKey(String playId) {
        return String.format(IDEMPOTENCY_KEY_PATTERN, playId);
    }
    
    /**
     * Generate game config cache key
     */
    public static String gameConfigKey(String gameId) {
        return String.format(GAME_CONFIG_KEY_PATTERN, gameId);
    }
}
