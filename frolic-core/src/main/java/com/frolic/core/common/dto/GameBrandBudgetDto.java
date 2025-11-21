package com.frolic.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for game brand budget allocation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameBrandBudgetDto {
    
    private String id;
    private String gameId;
    private String brandId;
    private String brandName;
    private Integer totalBudget;
    private Integer allocatedBudget;
    private Integer remainingBudget;
}
