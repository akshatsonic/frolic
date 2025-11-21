package com.frolic.core.common.dto;

import com.frolic.core.common.enums.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime issuedAt;
    private LocalDateTime redeemedAt;
    private LocalDateTime expiresAt;
    private String description;
    private Double value;
}
