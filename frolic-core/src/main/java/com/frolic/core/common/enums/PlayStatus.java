package com.frolic.core.common.enums;

/**
 * Represents the status of a play event
 */
public enum PlayStatus {
    /**
     * Play is queued for processing
     */
    QUEUED,
    
    /**
     * Play is being processed
     */
    PROCESSING,
    
    /**
     * Play resulted in a win
     */
    WINNER,
    
    /**
     * Play did not result in a win
     */
    LOSER,
    
    /**
     * Play processing failed
     */
    FAILED
}
