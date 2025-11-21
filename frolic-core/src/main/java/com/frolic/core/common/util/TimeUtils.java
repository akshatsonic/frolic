package com.frolic.core.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for time-related operations
 */
public final class TimeUtils {
    
    private TimeUtils() {
        // Utility class
    }
    
    /**
     * Get current timestamp
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
    
    /**
     * Calculate remaining seconds between now and end time
     */
    public static long remainingSeconds(LocalDateTime endTime) {
        return ChronoUnit.SECONDS.between(now(), endTime);
    }
    
    /**
     * Calculate elapsed seconds since start time
     */
    public static long elapsedSeconds(LocalDateTime startTime) {
        return ChronoUnit.SECONDS.between(startTime, now());
    }
    
    /**
     * Calculate duration between two LocalDateTime
     */
    public static Duration durationBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end);
    }
    
    /**
     * Check if current time is between start and end
     */
    public static boolean isBetween(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = now();
        return !now.isBefore(start) && !now.isAfter(end);
    }
    
}
