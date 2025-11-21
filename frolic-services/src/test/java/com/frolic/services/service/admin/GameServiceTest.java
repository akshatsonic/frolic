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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GameService
 */
@ExtendWith(MockitoExtension.class)
class GameServiceTest {
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private GameBrandBudgetRepository budgetRepository;
    
    @Mock
    private RedisBudgetStore redisBudgetStore;
    
    @Mock
    private BudgetSyncService budgetSyncService;
    
    @InjectMocks
    private GameService gameService;
    
    @Test
    void testGetAllGames_ReturnsAllGames() {
        GameEntity game1 = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        GameEntity game2 = createGameEntity("game-2", "Game 2", GameStatus.DRAFT);
        
        when(gameRepository.findAll()).thenReturn(Arrays.asList(game1, game2));
        when(budgetRepository.findByGameId(anyString())).thenReturn(Collections.emptyList());
        
        List<GameDto> result = gameService.getAllGames();
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("game-1");
        assertThat(result.get(1).getId()).isEqualTo("game-2");
        
        verify(gameRepository).findAll();
    }
    
    @Test
    void testGetGameById_ValidId_ReturnsGame() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Collections.emptyList());
        
        GameDto result = gameService.getGameById("game-1");
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("game-1");
        assertThat(result.getName()).isEqualTo("Game 1");
        
        verify(gameRepository).findById("game-1");
    }
    
    @Test
    void testGetGameById_InvalidId_ThrowsResourceNotFoundException() {
        when(gameRepository.findById("invalid-id")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> gameService.getGameById("invalid-id"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Game")
            .hasMessageContaining("invalid-id");
        
        verify(gameRepository).findById("invalid-id");
    }
    
    @Test
    void testGetGamesByCampaign_ReturnsCampaignGames() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        
        when(gameRepository.findByCampaignId("campaign-1")).thenReturn(Arrays.asList(game));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Collections.emptyList());
        
        List<GameDto> result = gameService.getGamesByCampaign("campaign-1");
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCampaignId()).isEqualTo("campaign-1");
        
        verify(gameRepository).findByCampaignId("campaign-1");
    }
    
    @Test
    void testGetGamesByStatus_ReturnsGamesWithStatus() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        
        when(gameRepository.findByStatusOrderByCreatedAtDesc(GameStatus.ACTIVE))
            .thenReturn(Arrays.asList(game));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Collections.emptyList());
        
        List<GameDto> result = gameService.getGamesByStatus(GameStatus.ACTIVE);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(GameStatus.ACTIVE);
        
        verify(gameRepository).findByStatusOrderByCreatedAtDesc(GameStatus.ACTIVE);
    }
    
    @Test
    void testCreateGame_ValidDto_CreatesGame() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusHours(1);
        LocalDateTime endTime = now.plusHours(11);
        
        CampaignEntity campaign = createCampaignEntity("campaign-1", now, now.plusDays(1));
        
        GameDto dto = GameDto.builder()
            .name("New Game")
            .campaignId("campaign-1")
            .startTime(startTime)
            .endTime(endTime)
            .probabilityType(ProbabilityType.TIME_BASED)
            .slotGranularitySeconds(5)
            .brandBudgets(Arrays.asList(
                GameBrandBudgetDto.builder().brandId("brand-1").totalBudget(100).build()
            ))
            .build();
        
        GameEntity savedEntity = createGameEntity("game-1", "New Game", GameStatus.DRAFT);
        
        when(campaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
        when(gameRepository.save(any(GameEntity.class))).thenReturn(savedEntity);
        when(budgetRepository.save(any(GameBrandBudgetEntity.class))).thenReturn(new GameBrandBudgetEntity());
        when(budgetRepository.findByGameId("game-1")).thenReturn(Collections.emptyList());
        
        GameDto result = gameService.createGame(dto);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("game-1");
        assertThat(result.getName()).isEqualTo("New Game");
        
        verify(campaignRepository).findById("campaign-1");
        verify(gameRepository).save(any(GameEntity.class));
        verify(budgetRepository).save(any(GameBrandBudgetEntity.class));
    }
    
    @Test
    void testCreateGame_EndTimeBeforeStartTime_ThrowsInvalidRequestException() {
        LocalDateTime now = LocalDateTime.now();
        
        GameDto dto = GameDto.builder()
            .name("Invalid Game")
            .campaignId("campaign-1")
            .startTime(now.plusHours(10))
            .endTime(now.plusHours(1)) // End before start
            .build();
        
        assertThatThrownBy(() -> gameService.createGame(dto))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("End time must be after start time");
        
        verify(gameRepository, never()).save(any(GameEntity.class));
    }
    
    @Test
    void testCreateGame_InvalidCampaignId_ThrowsResourceNotFoundException() {
        LocalDateTime now = LocalDateTime.now();
        
        GameDto dto = GameDto.builder()
            .name("New Game")
            .campaignId("invalid-campaign")
            .startTime(now.plusHours(1))
            .endTime(now.plusHours(11))
            .build();
        
        when(campaignRepository.findById("invalid-campaign")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> gameService.createGame(dto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Campaign")
            .hasMessageContaining("invalid-campaign");
        
        verify(gameRepository, never()).save(any(GameEntity.class));
    }
    
    @Test
    void testUpdateGame_ValidDtoAndDraftStatus_UpdatesGame() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusHours(1);
        LocalDateTime endTime = now.plusHours(11);
        
        GameEntity existingEntity = createGameEntity("game-1", "Old Game", GameStatus.DRAFT);
        CampaignEntity campaign = createCampaignEntity("campaign-1", now, now.plusDays(1));
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(existingEntity));
        when(campaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Collections.emptyList());
        
        GameDto dto = GameDto.builder()
            .name("Updated Game")
            .startTime(startTime)
            .endTime(endTime)
            .probabilityType(ProbabilityType.TIME_BASED)
            .slotGranularitySeconds(10)
            .build();
        
        GameDto result = gameService.updateGame("game-1", dto);
        
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Game");
        
        verify(gameRepository).findById("game-1");
        verify(gameRepository).save(any(GameEntity.class));
    }
    
    @Test
    void testUpdateGame_NonDraftStatus_ThrowsInvalidRequestException() {
        GameEntity existingEntity = createGameEntity("game-1", "Active Game", GameStatus.ACTIVE);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(existingEntity));
        
        GameDto dto = GameDto.builder()
            .name("Updated Game")
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now().plusHours(10))
            .build();
        
        assertThatThrownBy(() -> gameService.updateGame("game-1", dto))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Can only update games in DRAFT status");
        
        verify(gameRepository, never()).save(any(GameEntity.class));
    }
    
    @Test
    void testDeleteGame_ValidIdAndDraftStatus_DeletesGame() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.DRAFT);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        doNothing().when(gameRepository).deleteById("game-1");
        
        assertThatCode(() -> gameService.deleteGame("game-1"))
            .doesNotThrowAnyException();
        
        verify(gameRepository).findById("game-1");
        verify(gameRepository).deleteById("game-1");
    }
    
    @Test
    void testDeleteGame_NonDraftStatus_ThrowsInvalidRequestException() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        
        assertThatThrownBy(() -> gameService.deleteGame("game-1"))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Can only delete games in DRAFT status");
        
        verify(gameRepository, never()).deleteById(any());
    }
    
    @Test
    void testStartGame_ValidIdAndDraftStatus_StartsGame() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.DRAFT);
        GameBrandBudgetEntity budget = createBudgetEntity("game-1", "brand-1", 100);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Arrays.asList(budget));
        doNothing().when(redisBudgetStore).initializeBudget(anyString(), anyString(), anyInt());
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        GameDto result = gameService.startGame("game-1");
        
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GameStatus.ACTIVE);
        
        verify(gameRepository).findById("game-1");
        verify(budgetRepository, times(2)).findByGameId("game-1");
        verify(redisBudgetStore).initializeBudget("game-1", "brand-1", 100);
        verify(gameRepository).save(any(GameEntity.class));
    }
    
    @Test
    void testStartGame_NonDraftStatus_ThrowsInvalidRequestException() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        
        assertThatThrownBy(() -> gameService.startGame("game-1"))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Can only start games in DRAFT status");
        
        verify(redisBudgetStore, never()).initializeBudget(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testStopGame_ValidIdAndActiveStatus_StopsGame() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        GameBrandBudgetEntity budget = createBudgetEntity("game-1", "brand-1", 100);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Arrays.asList(budget));
        doNothing().when(budgetSyncService).syncBudgetsFromRedisToPostgres(anyString());
        doNothing().when(redisBudgetStore).clearBudget(anyString(), anyString());
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        GameDto result = gameService.stopGame("game-1");
        
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GameStatus.ENDED);
        
        verify(gameRepository).findById("game-1");
        verify(budgetSyncService).syncBudgetsFromRedisToPostgres("game-1");
        verify(redisBudgetStore).clearBudget("game-1", "brand-1");
        verify(gameRepository).save(any(GameEntity.class));
    }
    
    @Test
    void testStopGame_NonActiveStatus_ThrowsInvalidRequestException() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.DRAFT);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        
        assertThatThrownBy(() -> gameService.stopGame("game-1"))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Can only stop active games");
        
        verify(budgetSyncService, never()).syncBudgetsFromRedisToPostgres(anyString());
    }
    
    @Test
    void testPauseGame_ValidIdAndActiveStatus_PausesGame() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.ACTIVE);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Collections.emptyList());
        
        GameDto result = gameService.pauseGame("game-1");
        
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GameStatus.PAUSED);
        
        verify(gameRepository).findById("game-1");
        verify(gameRepository).save(any(GameEntity.class));
    }
    
    @Test
    void testResumeGame_ValidIdAndPausedStatus_ResumesGame() {
        GameEntity game = createGameEntity("game-1", "Game 1", GameStatus.PAUSED);
        
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetRepository.findByGameId("game-1")).thenReturn(Collections.emptyList());
        
        GameDto result = gameService.resumeGame("game-1");
        
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GameStatus.ACTIVE);
        
        verify(gameRepository).findById("game-1");
        verify(gameRepository).save(any(GameEntity.class));
    }
    
    private GameEntity createGameEntity(String id, String name, GameStatus status) {
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setCampaignId("campaign-1");
        entity.setStatus(status);
        entity.setStartTime(LocalDateTime.now());
        entity.setEndTime(LocalDateTime.now().plusHours(10));
        entity.setProbabilityType(ProbabilityType.TIME_BASED);
        entity.setSlotGranularitySeconds(5);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
    
    private CampaignEntity createCampaignEntity(String id, LocalDateTime startDate, LocalDateTime endDate) {
        CampaignEntity entity = new CampaignEntity();
        entity.setId(id);
        entity.setName("Test Campaign");
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        return entity;
    }
    
    private GameBrandBudgetEntity createBudgetEntity(String gameId, String brandId, int totalBudget) {
        GameBrandBudgetEntity entity = new GameBrandBudgetEntity();
        entity.setId("budget-1");
        entity.setGameId(gameId);
        entity.setBrandId(brandId);
        entity.setTotalBudget(totalBudget);
        entity.setAllocatedBudget(0);
        entity.setRemainingBudget(totalBudget);
        return entity;
    }
}
