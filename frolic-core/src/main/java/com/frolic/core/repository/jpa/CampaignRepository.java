package com.frolic.core.repository.jpa;

import com.frolic.core.common.enums.CampaignStatus;
import com.frolic.core.repository.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Campaign entity
 */
@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {
    
    List<CampaignEntity> findByStatus(CampaignStatus status);
    
    List<CampaignEntity> findByStatusOrderByCreatedAtDesc(CampaignStatus status);
    
    @Query("SELECT c FROM CampaignEntity c WHERE c.status = com.frolic.core.common.enums.CampaignStatus.DRAFT AND c.startDate <= :now")
    List<CampaignEntity> findDraftCampaignsReadyToStart(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM CampaignEntity c WHERE c.status = com.frolic.core.common.enums.CampaignStatus.ACTIVE AND c.endDate < :now")
    List<CampaignEntity> findActiveCampaignsReadyToEnd(@Param("now") LocalDateTime now);
}
