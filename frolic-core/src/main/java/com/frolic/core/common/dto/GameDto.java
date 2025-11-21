package com.frolic.core.common.dto;

import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.enums.ProbabilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ProbabilityType probabilityType;
    private Integer slotGranularitySeconds;
    private List<GameBrandBudgetDto> brandBudgets;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
