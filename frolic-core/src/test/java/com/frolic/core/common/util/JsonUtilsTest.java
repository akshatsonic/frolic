package com.frolic.core.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frolic.core.common.exception.TechnicalException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonUtils utility class
 */
class JsonUtilsTest {
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class TestObject {
        private String name;
        private int value;
        private LocalDateTime timestamp;
    }
    
    @Test
    void testGetObjectMapper_ReturnsNonNull() {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        assertNotNull(mapper);
    }
    
    @Test
    void testGetObjectMapper_ReturnsSameInstance() {
        ObjectMapper mapper1 = JsonUtils.getObjectMapper();
        ObjectMapper mapper2 = JsonUtils.getObjectMapper();
        assertSame(mapper1, mapper2, "Should return singleton instance");
    }
    
    @Test
    void testToJson_SerializesObjectSuccessfully() {
        TestObject obj = new TestObject("test", 123, LocalDateTime.of(2024, 1, 1, 12, 0));
        
        String json = JsonUtils.toJson(obj);
        
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }
    
    @Test
    void testToJson_HandlesNullValues() {
        TestObject obj = new TestObject(null, 0, null);
        
        String json = JsonUtils.toJson(obj);
        
        assertNotNull(json);
        assertTrue(json.contains("\"name\":null"));
    }
    
    @Test
    void testFromJson_DeserializesObjectSuccessfully() {
        String json = "{\"name\":\"test\",\"value\":123,\"timestamp\":\"2024-01-01T12:00:00\"}";
        
        TestObject obj = JsonUtils.fromJson(json, TestObject.class);
        
        assertNotNull(obj);
        assertEquals("test", obj.getName());
        assertEquals(123, obj.getValue());
        assertEquals(LocalDateTime.of(2024, 1, 1, 12, 0), obj.getTimestamp());
    }
    
    @Test
    void testFromJson_ThrowsExceptionForInvalidJson() {
        String invalidJson = "{invalid json}";
        
        assertThrows(TechnicalException.class, () -> 
            JsonUtils.fromJson(invalidJson, TestObject.class)
        );
    }
    
    @Test
    void testToPrettyJson_FormatsJson() {
        TestObject obj = new TestObject("test", 123, LocalDateTime.of(2024, 1, 1, 12, 0));
        
        String prettyJson = JsonUtils.toPrettyJson(obj);
        
        assertNotNull(prettyJson);
        assertTrue(prettyJson.contains("\n"), "Pretty JSON should contain newlines");
        assertTrue(prettyJson.contains("  "), "Pretty JSON should contain indentation");
    }
    
    @Test
    void testRoundTrip_PreservesData() {
        TestObject original = new TestObject("test", 456, LocalDateTime.of(2024, 6, 15, 10, 30));
        
        String json = JsonUtils.toJson(original);
        TestObject deserialized = JsonUtils.fromJson(json, TestObject.class);
        
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getValue(), deserialized.getValue());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
    }
    
    @Test
    void testFromJson_HandlesEmptyObject() {
        String json = "{}";
        
        TestObject obj = JsonUtils.fromJson(json, TestObject.class);
        
        assertNotNull(obj);
        assertNull(obj.getName());
        assertEquals(0, obj.getValue());
    }
}
