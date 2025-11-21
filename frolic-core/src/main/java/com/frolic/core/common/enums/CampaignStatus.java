package com.frolic.core.common.enums;

/**
 * Represents the status of a campaign
 */
public enum CampaignStatus {
    /**
     * Campaign is being created/configured
     */
    DRAFT,
    
    /**
     * Campaign is active and running games
     */
    ACTIVE,
    
    /**
     * Campaign has ended
     */
    ENDED
}
