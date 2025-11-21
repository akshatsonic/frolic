package com.frolic.core.repository.entity;

import com.frolic.core.common.enums.PlayStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Play event entity (audit log)
 */
@Entity
@Table(name = "play_events", indexes = {
    @Index(name = "idx_play_game", columnList = "game_id"),
    @Index(name = "idx_play_user", columnList = "user_id"),
    @Index(name = "idx_play_status", columnList = "status"),
    @Index(name = "idx_play_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayEventEntity extends BaseEntity {
    
    @Column(name = "game_id", nullable = false)
    private String gameId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlayStatus status;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "winner", nullable = false)
    private boolean winner = false;
    
    @Column(name = "coupon_id")
    private String couponId;
    
    @Column(name = "brand_id")
    private String brandId;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
