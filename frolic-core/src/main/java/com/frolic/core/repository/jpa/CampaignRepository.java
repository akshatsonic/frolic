package com.frolic.core.repository.jpa;

import com.frolic.core.common.enums.CampaignStatus;
import com.frolic.core.repository.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Campaign entity
 */
@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {
    
    List<CampaignEntity> findByStatus(CampaignStatus status);
    
    List<CampaignEntity> findByStatusOrderByCreatedAtDesc(CampaignStatus status);
}
