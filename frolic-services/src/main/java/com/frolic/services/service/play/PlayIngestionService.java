package com.frolic.services.service.play;

import com.frolic.core.common.dto.PlayEventDto;
import com.frolic.core.common.dto.PlayResultDto;
import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.enums.PlayStatus;
import com.frolic.core.common.exception.InvalidRequestException;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.common.util.IdGenerator;
import com.frolic.core.cache.store.RedisResultStore;
import com.frolic.core.messaging.producer.PlayEventProducer;
import com.frolic.core.common.enums.CampaignStatus;
import com.frolic.core.repository.entity.CampaignEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.jpa.CampaignRepository;
import com.frolic.core.repository.jpa.GameRepository;
import com.frolic.services.service.admin.UserService;
import com.frolic.services.controller.play.request.PlayRequest;
import com.frolic.services.controller.play.response.PlayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for play ingestion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlayIngestionService {
    
    private final PlayEventProducer playEventProducer;
    private final GameRepository gameRepository;
    private final CampaignRepository campaignRepository;
    private final UserService userService;
    private final RedisResultStore redisResultStore;
    
    /**
     * Submit a play request
     */
    public PlayResponse submitPlay(PlayRequest request) {
        // Validate user exists and is active
        if (!userService.isUserValid(request.getUserId())) {
            throw new InvalidRequestException("User does not exist or is inactive");
        }
        
        // Validate game exists and is active
        GameEntity game = gameRepository.findById(request.getGameId())
            .orElseThrow(() -> new ResourceNotFoundException("Game", request.getGameId()));
        
        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new InvalidRequestException("Game is not active");
        }
        
        // Validate campaign status - if campaign is completed, games cannot accept plays
        CampaignEntity campaign = campaignRepository.findById(game.getCampaignId())
            .orElseThrow(() -> new ResourceNotFoundException("Campaign", game.getCampaignId()));
        
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new InvalidRequestException("Campaign is inactive. No plays are allowed");
        }
        
        // Check if game time window is valid
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(game.getStartTime()) || now.isAfter(game.getEndTime())) {
            throw new InvalidRequestException("Game is not currently running");
        }
        
        // Generate play ID (using plain UUID to fit VARCHAR(36))
        String playId = IdGenerator.generateId();
        
        // Create play event
        PlayEventDto event = PlayEventDto.builder()
            .playId(playId)
            .gameId(request.getGameId())
            .userId(request.getUserId())
            .status(PlayStatus.QUEUED)
            .timestamp(now)
            .metadata(request.getMetadata())
            .build();
        
        // Publish to Kafka
        playEventProducer.publishPlayEvent(event);
        
        log.info("Play submitted: playId={}, userId={}, gameId={}", playId, request.getUserId(), request.getGameId());
        
        return PlayResponse.builder()
            .playId(playId)
            .gameId(request.getGameId())
            .userId(request.getUserId())
            .status(PlayStatus.QUEUED)
            .message("Play submitted successfully. Result will be available in 10 seconds.")
            .build();
    }
    
    /**
     * Get play result
     */
    public PlayResponse getPlayResult(String playId) {
        PlayResultDto result = redisResultStore.getResult(playId);
        
        if (result == null) {
            return PlayResponse.builder()
                .playId(playId)
                .status(PlayStatus.PROCESSING)
                .message("Result not yet available. Please try again in a few seconds.")
                .build();
        }
        
        return PlayResponse.builder()
            .playId(result.getPlayId())
            .gameId(result.getGameId())
            .userId(result.getUserId())
            .winner(result.isWinner())
            .couponCode(result.getCouponId())
            .brandName(result.getBrandName())
            .status(result.isWinner() ? PlayStatus.WINNER : PlayStatus.LOSER)
            .message(result.getMessage())
            .build();
    }
}
