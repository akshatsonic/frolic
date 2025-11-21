package com.frolic.core.common.dto;

import com.frolic.core.common.enums.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for coupon data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {
    
    private String id;
    private String code;
    private String brandId;
    private String brandName;
    private CouponStatus status;
    private String userId;
    private Instant issuedAt;
    private Instant redeemedAt;
    private Instant expiresAt;
    private String description;
    private Double value;
}
