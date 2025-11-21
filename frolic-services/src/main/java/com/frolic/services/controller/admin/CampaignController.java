package com.frolic.services.controller.admin;

import com.frolic.core.common.dto.CampaignDto;
import com.frolic.core.common.enums.CampaignStatus;
import com.frolic.services.service.admin.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for campaign management
 */
@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
@Slf4j
public class CampaignController {
    
    private final CampaignService campaignService;
    
    /**
     * Get all campaigns
     */
    @GetMapping
    public ResponseEntity<List<CampaignDto>> getAllCampaigns(
            @RequestParam(required = false) CampaignStatus status) {
        
        List<CampaignDto> campaigns = status != null 
            ? campaignService.getCampaignsByStatus(status)
            : campaignService.getAllCampaigns();
        
        return ResponseEntity.ok(campaigns);
    }
    
    /**
     * Get campaign by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampaignDto> getCampaignById(@PathVariable String id) {
        CampaignDto campaign = campaignService.getCampaignById(id);
        return ResponseEntity.ok(campaign);
    }
    
    /**
     * Create new campaign
     */
    @PostMapping
    public ResponseEntity<CampaignDto> createCampaign(@Valid @RequestBody CampaignDto dto) {
        log.info("Creating campaign: name={}", dto.getName());
        CampaignDto created = campaignService.createCampaign(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Update campaign
     */
    @PutMapping("/{id}")
    public ResponseEntity<CampaignDto> updateCampaign(
            @PathVariable String id,
            @Valid @RequestBody CampaignDto dto) {
        
        log.info("Updating campaign: id={}", id);
        CampaignDto updated = campaignService.updateCampaign(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete campaign
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable String id) {
        log.info("Deleting campaign: id={}", id);
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Activate campaign
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<CampaignDto> activateCampaign(@PathVariable String id) {
        log.info("Activating campaign: id={}", id);
        CampaignDto activated = campaignService.activateCampaign(id);
        return ResponseEntity.ok(activated);
    }
    
    /**
     * End campaign
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<CampaignDto> endCampaign(@PathVariable String id) {
        log.info("Ending campaign: id={}", id);
        CampaignDto ended = campaignService.endCampaign(id);
        return ResponseEntity.ok(ended);
    }
}
