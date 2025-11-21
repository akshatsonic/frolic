package com.frolic.core.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Game brand budget allocation entity
 */
@Entity
@Table(name = "game_brand_budgets", indexes = {
    @Index(name = "idx_gbb_game", columnList = "game_id"),
    @Index(name = "idx_gbb_brand", columnList = "brand_id"),
    @Index(name = "idx_gbb_game_brand", columnList = "game_id,brand_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameBrandBudgetEntity extends BaseEntity {
    
    @Column(name = "game_id", nullable = false)
    private String gameId;
    
    @Column(name = "brand_id", nullable = false)
    private String brandId;
    
    @Column(name = "total_budget", nullable = false)
    private Integer totalBudget;
    
    @Column(name = "allocated_budget", nullable = false)
    private Integer allocatedBudget = 0;
    
    @Column(name = "remaining_budget", nullable = false)
    private Integer remainingBudget;
    
    @PrePersist
    @PreUpdate
    protected void calculateRemainingBudget() {
        if (totalBudget != null && allocatedBudget != null) {
            remainingBudget = totalBudget - allocatedBudget;
        }
    }
}
