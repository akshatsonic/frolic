package com.frolic.core.common.dto;

import com.frolic.core.common.enums.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
