package com.frolic.core.common.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TimeUtils utility class
 */
class TimeUtilsTest {
    
    @Test
    void testNow_ReturnsCurrentTime() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = TimeUtils.now();
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(result);
        assertTrue(!result.isBefore(before) && !result.isAfter(after));
    }
    
    @Test
    void testRemainingSeconds_FutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        long remaining = TimeUtils.remainingSeconds(futureTime);
        
        assertTrue(remaining > 3500 && remaining <= 3600, 
            "Should be approximately 3600 seconds (1 hour)");
    }
    
    @Test
    void testRemainingSeconds_PastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        
        long remaining = TimeUtils.remainingSeconds(pastTime);
        
        assertTrue(remaining < 0, "Past time should return negative value");
    }
    
    @Test
    void testRemainingSeconds_CurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        
        long remaining = TimeUtils.remainingSeconds(now);
        
        assertTrue(remaining >= -1 && remaining <= 1, 
            "Current time should return value close to 0");
    }
    
    @Test
    void testElapsedSeconds_PastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(2);
        
        long elapsed = TimeUtils.elapsedSeconds(pastTime);
        
        assertTrue(elapsed > 7100 && elapsed <= 7200, 
            "Should be approximately 7200 seconds (2 hours)");
    }
    
    @Test
    void testElapsedSeconds_FutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        long elapsed = TimeUtils.elapsedSeconds(futureTime);
        
        assertTrue(elapsed < 0, "Future time should return negative value");
    }
    
    @Test
    void testDurationBetween_PositiveDuration() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 30);
        
        Duration duration = TimeUtils.durationBetween(start, end);
        
        assertNotNull(duration);
        assertEquals(150, duration.toMinutes());
    }
    
    @Test
    void testDurationBetween_NegativeDuration() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 10, 0);
        
        Duration duration = TimeUtils.durationBetween(start, end);
        
        assertNotNull(duration);
        assertTrue(duration.isNegative());
    }
    
    @Test
    void testDurationBetween_SameTime() {
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 10, 0);
        
        Duration duration = TimeUtils.durationBetween(time, time);
        
        assertNotNull(duration);
        assertTrue(duration.isZero());
    }
    
    @Test
    void testIsBetween_CurrentTimeWithinRange() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        
        boolean result = TimeUtils.isBetween(start, end);
        
        assertTrue(result, "Current time should be between past and future");
    }
    
    @Test
    void testIsBetween_CurrentTimeBeforeRange() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        
        boolean result = TimeUtils.isBetween(start, end);
        
        assertFalse(result, "Current time should not be in future range");
    }
    
    @Test
    void testIsBetween_CurrentTimeAfterRange() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        
        boolean result = TimeUtils.isBetween(start, end);
        
        assertFalse(result, "Current time should not be in past range");
    }
    
    @Test
    void testIsBetween_AtStartTime() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);
        
        // Small delay to ensure we're testing boundary
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        boolean result = TimeUtils.isBetween(start, end);
        
        assertTrue(result, "Current time at start boundary should be included");
    }
}
