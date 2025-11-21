package com.frolic.core.common.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    public static Instant now() {
        return Instant.now();
    }
    
    /**
     * Calculate remaining seconds between now and end time
     */
    public static long remainingSeconds(Instant endTime) {
        return ChronoUnit.SECONDS.between(now(), endTime);
    }
    
    /**
     * Calculate elapsed seconds since start time
     */
    public static long elapsedSeconds(Instant startTime) {
        return ChronoUnit.SECONDS.between(startTime, now());
    }
    
    /**
     * Calculate duration between two instants
     */
    public static Duration durationBetween(Instant start, Instant end) {
        return Duration.between(start, end);
    }
    
    /**
     * Check if current time is between start and end
     */
    public static boolean isBetween(Instant start, Instant end) {
        Instant now = now();
        return !now.isBefore(start) && !now.isAfter(end);
    }
    
    /**
     * Convert Instant to LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
    
    /**
     * Convert LocalDateTime to Instant
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
