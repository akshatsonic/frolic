package com.frolic.core.common.util;

import java.util.UUID;

/**
 * Utility class for generating unique identifiers
 */
public final class IdGenerator {
    
    private IdGenerator() {
        // Utility class
    }
    
    /**
     * Generate a new UUID as a string
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a UUID without hyphens
     */
    public static String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a prefixed ID
     */
    public static String generatePrefixedId(String prefix) {
        return prefix + "-" + generateId();
    }
}
