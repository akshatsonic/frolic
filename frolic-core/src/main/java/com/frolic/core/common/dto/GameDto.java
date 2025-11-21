package com.frolic.core.common.dto;

import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.enums.ProbabilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for game data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {
    
    private String id;
    private String name;
    private String campaignId;
    private GameStatus status;
    private Instant startTime;
    private Instant endTime;
    private ProbabilityType probabilityType;
    private Integer slotGranularitySeconds;
    private List<GameBrandBudgetDto> brandBudgets;
    private Instant createdAt;
    private Instant updatedAt;
}
