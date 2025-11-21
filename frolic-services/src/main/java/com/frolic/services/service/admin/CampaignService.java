package com.frolic.services.service.admin;

import com.frolic.core.common.dto.CampaignDto;
import com.frolic.core.common.enums.CampaignStatus;
import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.repository.entity.CampaignEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.jpa.CampaignRepository;
import com.frolic.core.repository.jpa.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for campaign management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {
    
    private final CampaignRepository campaignRepository;
    private final GameRepository gameRepository;
    private final GameService gameService;
    
    /**
     * Get all campaigns
     */
    public List<CampaignDto> getAllCampaigns() {
        return campaignRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get campaign by ID
     */
    public CampaignDto getCampaignById(String id) {
        CampaignEntity entity = campaignRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
        return toDto(entity);
    }
    
    /**
     * Get campaigns by status
     */
    public List<CampaignDto> getCampaignsByStatus(CampaignStatus status) {
        return campaignRepository.findByStatusOrderByCreatedAtDesc(status).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Create a new campaign
     */
    @Transactional
    public CampaignDto createCampaign(CampaignDto dto) {
        CampaignEntity entity = new CampaignEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : CampaignStatus.DRAFT);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        
        entity = campaignRepository.save(entity);
        log.info("Created campaign: id={}, name={}", entity.getId(), entity.getName());
        
        return toDto(entity);
    }
    
    /**
     * Update existing campaign
     */
    @Transactional
    public CampaignDto updateCampaign(String id, CampaignDto dto) {
        CampaignEntity entity = campaignRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
        
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        
        entity = campaignRepository.save(entity);
        log.info("Updated campaign: id={}, name={}", entity.getId(), entity.getName());
        
        return toDto(entity);
    }
    
    /**
     * Delete campaign
     */
    @Transactional
    public void deleteCampaign(String id) {
        if (!campaignRepository.existsById(id)) {
            throw new ResourceNotFoundException("Campaign", id);
        }
        campaignRepository.deleteById(id);
        log.info("Deleted campaign: id={}", id);
    }
    
    /**
     * Activate campaign
     */
    @Transactional
    public CampaignDto activateCampaign(String id) {
        CampaignEntity entity = campaignRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
        
        entity.setStatus(CampaignStatus.ACTIVE);
        entity = campaignRepository.save(entity);
        log.info("Activated campaign: id={}", id);
        
        return toDto(entity);
    }
    
    /**
     * End campaign - cascade to all games
     */
    @Transactional
    public CampaignDto endCampaign(String id) {
        CampaignEntity entity = campaignRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
        
        // End all active games in this campaign
        List<GameEntity> games = gameRepository.findByCampaignId(id);
        for (GameEntity game : games) {
            if (game.getStatus() == GameStatus.ACTIVE) {
                try {
                    gameService.stopGame(game.getId());
                    log.info("Stopped game: id={} as part of campaign completion", game.getId());
                } catch (Exception e) {
                    log.error("Failed to stop game: id={} during campaign completion", game.getId(), e);
                }
            }
        }
        
        entity.setStatus(CampaignStatus.ENDED);
        entity = campaignRepository.save(entity);
        log.info("Ended campaign: id={}, all games stopped", id);
        
        return toDto(entity);
    }
    
    private CampaignDto toDto(CampaignEntity entity) {
        return CampaignDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .status(entity.getStatus())
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
