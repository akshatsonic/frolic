package com.frolic.core.common.enums;

/**
 * Represents different probability calculation strategies
 */
public enum ProbabilityType {
    /**
     * Time-based probability calculation (default)
     * Distributes rewards evenly over game duration
     */
    TIME_BASED,
    
    /**
     * Slot-based probability calculation
     * Uses remaining slots to calculate probability
     */
    SLOT_BASED,
    
    /**
     * Custom probability configuration
     * Uses fixed or configured probability values
     */
    CUSTOM
}
