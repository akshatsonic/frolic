package com.frolic.core.repository.jpa;

import com.frolic.core.common.enums.CouponStatus;
import com.frolic.core.repository.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Coupon entity
 */
@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, String> {
    
    Optional<CouponEntity> findByCode(String code);
    
    List<CouponEntity> findByBrandIdAndStatus(String brandId, CouponStatus status);
    
    List<CouponEntity> findByUserId(String userId);
    
    @Query("SELECT c FROM CouponEntity c WHERE c.brandId = :brandId AND c.status = :status ORDER BY RANDOM() LIMIT 1")
    Optional<CouponEntity> findRandomAvailableCoupon(@Param("brandId") String brandId, @Param("status") CouponStatus status);
    
    long countByBrandIdAndStatus(String brandId, CouponStatus status);
}
