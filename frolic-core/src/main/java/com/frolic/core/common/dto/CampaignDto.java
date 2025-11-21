package com.frolic.core.common.dto;

import com.frolic.core.common.enums.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for campaign data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDto {
    
    private String id;
    private String name;
    private String description;
    private CampaignStatus status;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;
    private Instant updatedAt;
}
