package com.frolic.core.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IdGenerator utility class
 */
class IdGeneratorTest {
    
    @Test
    void testGenerateId_ReturnsValidUUID() {
        String id = IdGenerator.generateId();
        
        assertNotNull(id);
        assertDoesNotThrow(() -> UUID.fromString(id));
        assertTrue(id.contains("-"));
    }
    
    @Test
    void testGenerateId_GeneratesUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(IdGenerator.generateId());
        }
        
        assertEquals(1000, ids.size(), "All generated IDs should be unique");
    }
    
    @Test
    void testGenerateShortId_ReturnsValidUUIDWithoutHyphens() {
        String id = IdGenerator.generateShortId();
        
        assertNotNull(id);
        assertFalse(id.contains("-"), "Short ID should not contain hyphens");
        assertEquals(32, id.length(), "Short ID should be 32 characters (UUID without hyphens)");
    }
    
    @Test
    void testGenerateShortId_GeneratesUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(IdGenerator.generateShortId());
        }
        
        assertEquals(1000, ids.size(), "All generated short IDs should be unique");
    }
    
    @Test
    void testGeneratePrefixedId_ContainsPrefix() {
        String prefix = "PLAY";
        String id = IdGenerator.generatePrefixedId(prefix);
        
        assertNotNull(id);
        assertTrue(id.startsWith(prefix + "-"));
    }
    
    @Test
    void testGeneratePrefixedId_GeneratesValidUUID() {
        String prefix = "GAME";
        String id = IdGenerator.generatePrefixedId(prefix);
        
        String uuidPart = id.substring(prefix.length() + 1);
        assertDoesNotThrow(() -> UUID.fromString(uuidPart));
    }
    
    @Test
    void testGeneratePrefixedId_WithEmptyPrefix() {
        String id = IdGenerator.generatePrefixedId("");
        
        assertNotNull(id);
        assertTrue(id.startsWith("-"));
    }
    
    @Test
    void testGeneratePrefixedId_GeneratesUniqueIds() {
        String prefix = "TEST";
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(IdGenerator.generatePrefixedId(prefix));
        }
        
        assertEquals(1000, ids.size(), "All generated prefixed IDs should be unique");
    }
}
