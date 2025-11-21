package com.frolic.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for play result data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayResultDto {
    
    private String playId;
    private String gameId;
    private String userId;
    private boolean winner;
    private String couponId;
    private String brandId;
    private String brandName;
    private Instant timestamp;
    private String message;
}
