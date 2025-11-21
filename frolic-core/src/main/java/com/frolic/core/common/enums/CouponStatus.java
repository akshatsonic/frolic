package com.frolic.core.common.enums;

/**
 * Represents the lifecycle status of a coupon
 */
public enum CouponStatus {
    /**
     * Coupon is available in the pool
     */
    AVAILABLE,
    
    /**
     * Coupon is reserved for allocation
     */
    RESERVED,
    
    /**
     * Coupon has been issued to a user
     */
    ISSUED,
    
    /**
     * Coupon has been redeemed
     */
    REDEEMED,
    
    /**
     * Coupon has expired
     */
    EXPIRED
}
