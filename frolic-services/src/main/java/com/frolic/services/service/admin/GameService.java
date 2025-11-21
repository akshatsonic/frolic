package com.frolic.services.service.admin;

import com.frolic.core.cache.store.RedisBudgetStore;
import com.frolic.core.common.dto.GameBrandBudgetDto;
import com.frolic.core.common.dto.GameDto;
import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.enums.ProbabilityType;
import com.frolic.core.common.exception.InvalidRequestException;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.repository.entity.CampaignEntity;
import com.frolic.core.repository.entity.GameBrandBudgetEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.jpa.CampaignRepository;
import com.frolic.core.repository.jpa.GameBrandBudgetRepository;
import com.frolic.core.repository.jpa.GameRepository;
import com.frolic.services.service.budget.BudgetSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for game management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {
    
    private final GameRepository gameRepository;
    private final CampaignRepository campaignRepository;
    private final GameBrandBudgetRepository budgetRepository;
    private final RedisBudgetStore redisBudgetStore;
    private final BudgetSyncService budgetSyncService;
    
    /**
     * Get all games
     */
    public List<GameDto> getAllGames() {
        return gameRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get game by ID
     */
    public GameDto getGameById(String id) {
        GameEntity entity = gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", id));
        return toDto(entity);
    }
    
    /**
     * Get games by campaign
     */
    public List<GameDto> getGamesByCampaign(String campaignId) {
        return gameRepository.findByCampaignId(campaignId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get games by status
     */
    public List<GameDto> getGamesByStatus(GameStatus status) {
        return gameRepository.findByStatusOrderByCreatedAtDesc(status).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Create a new game
     */
    @Transactional
    public GameDto createGame(GameDto dto) {
        // Validate time window
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new InvalidRequestException("End time must be after start time");
        }
        
        // Validate game times are within campaign date range
        CampaignEntity campaign = campaignRepository.findById(dto.getCampaignId())
            .orElseThrow(() -> new ResourceNotFoundException("Campaign", dto.getCampaignId()));
        
        if (dto.getStartTime().isBefore(campaign.getStartDate())) {
            throw new InvalidRequestException("Game start time must be on or after campaign start date");
        }
        
        if (dto.getEndTime().isAfter(campaign.getEndDate())) {
            throw new InvalidRequestException("Game end time must be on or before campaign end date");
        }
        
        GameEntity entity = new GameEntity();
        entity.setName(dto.getName());
        entity.setCampaignId(dto.getCampaignId());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : GameStatus.DRAFT);
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setProbabilityType(dto.getProbabilityType() != null ? dto.getProbabilityType() : ProbabilityType.TIME_BASED);
        entity.setSlotGranularitySeconds(dto.getSlotGranularitySeconds() != null ? dto.getSlotGranularitySeconds() : 5);
        
        entity = gameRepository.save(entity);
        
        // Create brand budgets
        if (dto.getBrandBudgets() != null && !dto.getBrandBudgets().isEmpty()) {
            for (GameBrandBudgetDto budgetDto : dto.getBrandBudgets()) {
                GameBrandBudgetEntity budgetEntity = new GameBrandBudgetEntity();
                budgetEntity.setGameId(entity.getId());
                budgetEntity.setBrandId(budgetDto.getBrandId());
                budgetEntity.setTotalBudget(budgetDto.getTotalBudget());
                budgetEntity.setAllocatedBudget(0);
                budgetEntity.setRemainingBudget(budgetDto.getTotalBudget());
                budgetRepository.save(budgetEntity);
            }
        }
        
        log.info("Created game: id={}, name={}, campaign={}", entity.getId(), entity.getName(), entity.getCampaignId());
        
        return toDto(entity);
    }
    
    /**
     * Update existing game
     */
    @Transactional
    public GameDto updateGame(String id, GameDto dto) {
        GameEntity entity = gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", id));
        
        // Can only update draft games
        if (entity.getStatus() != GameStatus.DRAFT) {
            throw new InvalidRequestException("Can only update games in DRAFT status");
        }
        
        // Validate time window
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new InvalidRequestException("End time must be after start time");
        }
        
        // Validate game times are within campaign date range
        String campaignId=entity.getCampaignId();
        CampaignEntity campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
        
        if (dto.getStartTime().isBefore(campaign.getStartDate())) {
            throw new InvalidRequestException("Game start time must be on or after campaign start date");
        }
        
        if (dto.getEndTime().isAfter(campaign.getEndDate())) {
            throw new InvalidRequestException("Game end time must be on or before campaign end date");
        }
        
        entity.setName(dto.getName());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setProbabilityType(dto.getProbabilityType());
        entity.setSlotGranularitySeconds(dto.getSlotGranularitySeconds());
        
        entity = gameRepository.save(entity);
        log.info("Updated game: id={}, name={}", entity.getId(), entity.getName());
        
        return toDto(entity);
    }
    
    /**
     * Delete game
     */
    @Transactional
    public void deleteGame(String id) {
        GameEntity entity = gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", id));
        
        // Can only delete draft games
        if (entity.getStatus() != GameStatus.DRAFT) {
            throw new InvalidRequestException("Can only delete games in DRAFT status");
        }
        
        gameRepository.deleteById(id);
        log.info("Deleted game: id={}", id);
    }
    
    /**
     * Start game - loads budgets to Redis
     */
    @Transactional
    public GameDto startGame(String id) {
        GameEntity entity = gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", id));
        
        if (entity.getStatus() != GameStatus.DRAFT) {
            throw new InvalidRequestException("Can only start games in DRAFT status");
        }
        
        // Load budgets to Redis
        List<GameBrandBudgetEntity> budgets = budgetRepository.findByGameId(id);
        for (GameBrandBudgetEntity budget : budgets) {
            redisBudgetStore.initializeBudget(id, budget.getBrandId(), budget.getTotalBudget());
        }
        
        entity.setStatus(GameStatus.ACTIVE);
        entity = gameRepository.save(entity);
        
        log.info("Started game: id={}, budgets loaded to Redis", id);
        
        return toDto(entity);
    }
    
    /**
     * Stop game - sync budgets and cleanup
     */
    @Transactional
    public GameDto stopGame(String id) {
        GameEntity entity = gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", id));
        
        if (entity.getStatus() != GameStatus.ACTIVE) {
            throw new InvalidRequestException("Can only stop active games");
        }
        
        // Sync budgets from Redis to PostgreSQL before clearing
        budgetSyncService.syncBudgetsFromRedisToPostgres(id);
        
        // Clear budgets from Redis
        List<GameBrandBudgetEntity> budgets = budgetRepository.findByGameId(id);
        for (GameBrandBudgetEntity budget : budgets) {
            redisBudgetStore.clearBudget(id, budget.getBrandId());
        }
        
        entity.setStatus(GameStatus.ENDED);
        entity = gameRepository.save(entity);
        
        log.info("Stopped game: id={}, budgets synced to PostgreSQL", id);
        
        return toDto(entity);
    }
    
    /**
     * Pause game
     */
    @Transactional
    public GameDto pauseGame(String id) {
        GameEntity entity = gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", id));
        
        if (entity.getStatus() != GameStatus.ACTIVE) {
            throw new InvalidRequestException("Can only pause active games");
        }
        
        entity.setStatus(GameStatus.PAUSED);
        entity = gameRepository.save(entity);
        
        log.info("Paused game: id={}", id);
        
        return toDto(entity);
    }
    
    /**
     * Resume game
     */
    @Transactional
    public GameDto resumeGame(String id) {
        GameEntity entity = gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", id));
        
        if (entity.getStatus() != GameStatus.PAUSED) {
            throw new InvalidRequestException("Can only resume paused games");
        }
        
        entity.setStatus(GameStatus.ACTIVE);
        entity = gameRepository.save(entity);
        
        log.info("Resumed game: id={}", id);
        
        return toDto(entity);
    }
    
    private GameDto toDto(GameEntity entity) {
        List<GameBrandBudgetDto> budgetDtos = budgetRepository.findByGameId(entity.getId()).stream()
            .map(b -> GameBrandBudgetDto.builder()
                .id(b.getId())
                .gameId(b.getGameId())
                .brandId(b.getBrandId())
                .totalBudget(b.getTotalBudget())
                .allocatedBudget(b.getAllocatedBudget())
                .remainingBudget(b.getRemainingBudget())
                .build())
            .collect(Collectors.toList());
        
        return GameDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .campaignId(entity.getCampaignId())
            .status(entity.getStatus())
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .probabilityType(entity.getProbabilityType())
            .slotGranularitySeconds(entity.getSlotGranularitySeconds())
            .brandBudgets(budgetDtos)
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
