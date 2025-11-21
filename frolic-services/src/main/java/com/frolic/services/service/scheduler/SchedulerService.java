package com.frolic.services.service.scheduler;

import com.frolic.core.repository.entity.CampaignEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.jpa.CampaignRepository;
import com.frolic.core.repository.jpa.GameRepository;
import com.frolic.services.service.admin.CampaignService;
import com.frolic.services.service.admin.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for automatic lifecycle management
 * - Starts campaigns at their scheduled start date
 * - Ends campaigns at their scheduled end date
 * - Starts games at their scheduled start time
 * - Ends games at their scheduled end time
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {
    
    private final CampaignRepository campaignRepository;
    private final GameRepository gameRepository;
    private final CampaignService campaignService;
    private final GameService gameService;
    
    /**
     * Check and start campaigns every minute
     * Looks for DRAFT campaigns whose start date has arrived
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at :00 seconds
    public void autoStartCampaigns() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Find DRAFT campaigns whose start date has arrived (filtered at DB level)
            List<CampaignEntity> campaignsToStart = campaignRepository.findDraftCampaignsReadyToStart(now);
            
            if (!campaignsToStart.isEmpty()) {
                log.info("Found {} campaigns ready to start", campaignsToStart.size());
            }
            
            for (CampaignEntity campaign : campaignsToStart) {
                try {
                    campaignService.activateCampaign(campaign.getId());
                    log.info("Auto-started campaign: id={}, name={}", campaign.getId(), campaign.getName());
                } catch (Exception e) {
                    log.error("Failed to auto-start campaign: id={}, name={}", 
                        campaign.getId(), campaign.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in autoStartCampaigns scheduler", e);
        }
    }
    
    /**
     * Check and end campaigns every minute
     * Looks for ACTIVE campaigns whose end date has passed
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at :00 seconds
    public void autoEndCampaigns() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Find ACTIVE campaigns whose end date has passed (filtered at DB level)
            List<CampaignEntity> campaignsToEnd = campaignRepository.findActiveCampaignsReadyToEnd(now);
            
            if (!campaignsToEnd.isEmpty()) {
                log.info("Found {} campaigns ready to end", campaignsToEnd.size());
            }
            
            for (CampaignEntity campaign : campaignsToEnd) {
                try {
                    campaignService.endCampaign(campaign.getId());
                    log.info("Auto-ended campaign: id={}, name={} (also stopped all games)", 
                        campaign.getId(), campaign.getName());
                } catch (Exception e) {
                    log.error("Failed to auto-end campaign: id={}, name={}", 
                        campaign.getId(), campaign.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in autoEndCampaigns scheduler", e);
        }
    }
    
    /**
     * Check and start games every minute
     * Looks for DRAFT games whose start time has arrived
     */
    @Scheduled(initialDelay = 3000, fixedRate = 60000) // Every minute at :30 seconds
    public void autoStartGames() {
        try {
            log.info("Auto-starting games...");
            LocalDateTime now = LocalDateTime.now();
            
            // Find DRAFT games whose start time has arrived (filtered at DB level)
            List<GameEntity> gamesToStart = gameRepository.findDraftGamesReadyToStart(now);
            
            if (!gamesToStart.isEmpty()) {
                log.info("Found {} games ready to start", gamesToStart.size());
            }
            
            for (GameEntity game : gamesToStart) {
                try {
                    gameService.startGame(game.getId());
                    log.info("Auto-started game: id={}, name={}", game.getId(), game.getName());
                } catch (Exception e) {
                    log.error("Failed to auto-start game: id={}, name={}", 
                        game.getId(), game.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in autoStartGames scheduler", e);
        }
    }
    
    /**
     * Check and end games every minute
     * Looks for ACTIVE games whose end time has passed
     */
    @Scheduled(initialDelay = 3000, fixedRate = 60000) // Every minute at :30 seconds
    public void autoEndGames() {
        try {
            log.info("Auto-stopping games...");
            LocalDateTime now = LocalDateTime.now();
            
            // Find ACTIVE games whose end time has passed (filtered at DB level)
            List<GameEntity> gamesToEnd = gameRepository.findActiveGamesReadyToEnd(now);
            
            if (!gamesToEnd.isEmpty()) {
                log.info("Found {} games ready to end", gamesToEnd.size());
            }
            
            for (GameEntity game : gamesToEnd) {
                try {
                    gameService.stopGame(game.getId());
                    log.info("Auto-ended game: id={}, name={}", game.getId(), game.getName());
                } catch (Exception e) {
                    log.error("Failed to auto-end game: id={}, name={}", 
                        game.getId(), game.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in autoEndGames scheduler", e);
        }
    }
}
