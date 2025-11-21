package com.frolic.core.repository.entity;

import com.frolic.core.common.enums.CouponStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Coupon entity
 */
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupon_brand", columnList = "brand_id"),
    @Index(name = "idx_coupon_status", columnList = "status"),
    @Index(name = "idx_coupon_user", columnList = "user_id"),
    @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponEntity extends BaseEntity {
    
    @Column(name = "code", nullable = false, unique = true)
    private String code;
    
    @Column(name = "brand_id", nullable = false)
    private String brandId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
    
    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "value")
    private Double value;
}
