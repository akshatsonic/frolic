package com.frolic.core.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KafkaTopics constant class
 */
class KafkaTopicsTest {
    
    @Test
    void testPlayEvents_HasCorrectValue() {
        assertEquals("play-events", KafkaTopics.PLAY_EVENTS);
    }
    
    @Test
    void testAllocationResults_HasCorrectValue() {
        assertEquals("allocation-results", KafkaTopics.ALLOCATION_RESULTS);
    }
    
    @Test
    void testCouponIssued_HasCorrectValue() {
        assertEquals("coupon-issued", KafkaTopics.COUPON_ISSUED);
    }
    
    @Test
    void testGameLifecycle_HasCorrectValue() {
        assertEquals("game-lifecycle", KafkaTopics.GAME_LIFECYCLE);
    }
    
    @Test
    void testAllTopics_AreUnique() {
        String[] topics = {
            KafkaTopics.PLAY_EVENTS,
            KafkaTopics.ALLOCATION_RESULTS,
            KafkaTopics.COUPON_ISSUED,
            KafkaTopics.GAME_LIFECYCLE
        };
        
        for (int i = 0; i < topics.length; i++) {
            for (int j = i + 1; j < topics.length; j++) {
                assertNotEquals(topics[i], topics[j], 
                    "Topics should be unique: " + topics[i] + " vs " + topics[j]);
            }
        }
    }
    
    @Test
    void testAllTopics_AreNotEmpty() {
        assertNotNull(KafkaTopics.PLAY_EVENTS);
        assertNotNull(KafkaTopics.ALLOCATION_RESULTS);
        assertNotNull(KafkaTopics.COUPON_ISSUED);
        assertNotNull(KafkaTopics.GAME_LIFECYCLE);
        
        assertFalse(KafkaTopics.PLAY_EVENTS.isEmpty());
        assertFalse(KafkaTopics.ALLOCATION_RESULTS.isEmpty());
        assertFalse(KafkaTopics.COUPON_ISSUED.isEmpty());
        assertFalse(KafkaTopics.GAME_LIFECYCLE.isEmpty());
    }
    
    @Test
    void testTopicNamingConvention_UsesHyphens() {
        assertTrue(KafkaTopics.PLAY_EVENTS.contains("-"));
        assertTrue(KafkaTopics.ALLOCATION_RESULTS.contains("-"));
        assertTrue(KafkaTopics.COUPON_ISSUED.contains("-"));
        assertTrue(KafkaTopics.GAME_LIFECYCLE.contains("-"));
    }
}
