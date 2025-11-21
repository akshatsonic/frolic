package com.frolic.services.config;

import com.frolic.core.common.exception.BusinessException;
import com.frolic.core.common.exception.ConcurrencyException;
import com.frolic.core.common.exception.InvalidRequestException;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.common.exception.TechnicalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.frolic.services.config.GlobalExceptionHandler.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }
    
    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = 
            new ResourceNotFoundException("Game", "game-123");
        
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleResourceNotFound(exception);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getErrorCode());
        assertTrue(body.getMessage().contains("Game"));
        assertTrue(body.getMessage().contains("game-123"));
        assertNotNull(body.getTimestamp());
    }
    
    @Test
    void testHandleInvalidRequestException() {
        InvalidRequestException exception = 
            new InvalidRequestException("Invalid game configuration");
        
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleBusinessException(new BusinessException("INVALID_REQUEST", exception.getMessage()));
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_REQUEST", body.getErrorCode());
        assertEquals("Invalid game configuration", body.getMessage());
    }
    
    @Test
    void testHandleBusinessException() {
        BusinessException exception = 
            new BusinessException("BIZ_ERROR", "Business rule violated");
        
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleBusinessException(exception);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("BIZ_ERROR", body.getErrorCode());
        assertTrue(body.getMessage().contains("Business rule violated"));
    }
    
    @Test
    void testHandleConcurrencyException() {
        ConcurrencyException exception = 
            new ConcurrencyException("Budget decrement failed");
        
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleTechnicalException(new TechnicalException("CONCURRENCY_ERROR", exception.getMessage()));
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("CONCURRENCY_ERROR", body.getErrorCode());
        assertEquals("Budget decrement failed", body.getMessage());
    }
    
    @Test
    void testHandleGenericException() {
        RuntimeException exception = new RuntimeException("Unexpected error");
        
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleGenericException(exception);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.getErrorCode());
        assertEquals("An unexpected error occurred", body.getMessage());
    }
    
    @Test
    void testResponseBodyContainsTimestamp() {
        InvalidRequestException exception = new InvalidRequestException("Test");
        
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleBusinessException(new BusinessException("INVALID", exception.getMessage()));
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getTimestamp());
    }
    
    @Test
    void testResponseBodyStructure() {
        ResourceNotFoundException exception = 
            new ResourceNotFoundException("User", "user-1");
        
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleResourceNotFound(exception);
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getTimestamp());
        assertNotNull(body.getErrorCode());
        assertNotNull(body.getMessage());
    }
}
