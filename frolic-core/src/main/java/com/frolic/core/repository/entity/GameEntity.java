package com.frolic.core.repository.entity;

import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.enums.ProbabilityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Game entity
 */
@Entity
@Table(name = "games", indexes = {
    @Index(name = "idx_game_campaign", columnList = "campaign_id"),
    @Index(name = "idx_game_status", columnList = "status"),
    @Index(name = "idx_game_times", columnList = "start_time,end_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameEntity extends BaseEntity {
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "campaign_id", nullable = false)
    private String campaignId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameStatus status;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "probability_type", nullable = false)
    private ProbabilityType probabilityType;
    
    @Column(name = "slot_granularity_seconds", nullable = false)
    private Integer slotGranularitySeconds;
    
    @OneToMany(mappedBy = "gameId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameBrandBudgetEntity> brandBudgets = new ArrayList<>();
}
