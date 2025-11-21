package com.frolic.core.common.constant;

/**
 * Kafka topic names used across the application
 */
public final class KafkaTopics {
    
    private KafkaTopics() {
        // Utility class
    }
    
    /**
     * Topic for play events
     */
    public static final String PLAY_EVENTS = "play-events";
    
    /**
     * Topic for allocation results (audit trail)
     */
    public static final String ALLOCATION_RESULTS = "allocation-results";
    
    /**
     * Topic for coupon issuance events
     */
    public static final String COUPON_ISSUED = "coupon-issued";
    
    /**
     * Topic for game lifecycle events (start/stop)
     */
    public static final String GAME_LIFECYCLE = "game-lifecycle";
}
