package com.frolic.core.common.enums;

/**
 * Represents the lifecycle status of a game
 */
public enum GameStatus {
    /**
     * Game is being created/configured
     */
    DRAFT,
    
    /**
     * Game is live and accepting plays
     */
    ACTIVE,
    
    /**
     * Game is temporarily paused
     */
    PAUSED,
    
    /**
     * Game has reached its end time
     */
    ENDED,
    
    /**
     * Game was cancelled before completion
     */
    CANCELLED
}
