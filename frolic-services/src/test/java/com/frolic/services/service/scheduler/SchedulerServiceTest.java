package com.frolic.services.service.scheduler;

import com.frolic.core.repository.entity.CampaignEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.jpa.CampaignRepository;
import com.frolic.core.repository.jpa.GameRepository;
import com.frolic.services.service.admin.CampaignService;
import com.frolic.services.service.admin.GameService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchedulerService
 */
@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {
    
    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private CampaignService campaignService;
    
    @Mock
    private GameService gameService;
    
    @InjectMocks
    private SchedulerService schedulerService;
    
    @Test
    void testAutoStartCampaigns_WithCampaignsReadyToStart_StartsThem() {
        CampaignEntity campaign1 = createCampaignEntity("campaign-1", "Campaign 1");
        CampaignEntity campaign2 = createCampaignEntity("campaign-2", "Campaign 2");
        
        when(campaignRepository.findDraftCampaignsReadyToStart(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(campaign1, campaign2));
        when(campaignService.activateCampaign(anyString())).thenReturn(null);
        
        schedulerService.autoStartCampaigns();
        
        verify(campaignRepository).findDraftCampaignsReadyToStart(any(LocalDateTime.class));
        verify(campaignService).activateCampaign("campaign-1");
        verify(campaignService).activateCampaign("campaign-2");
    }
    
    @Test
    void testAutoStartCampaigns_WithNoCampaignsReady_DoesNothing() {
        when(campaignRepository.findDraftCampaignsReadyToStart(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        schedulerService.autoStartCampaigns();
        
        verify(campaignRepository).findDraftCampaignsReadyToStart(any(LocalDateTime.class));
        verify(campaignService, never()).activateCampaign(anyString());
    }
    
    @Test
    void testAutoStartCampaigns_WhenOneFails_ContinuesWithOthers() {
        CampaignEntity campaign1 = createCampaignEntity("campaign-1", "Campaign 1");
        CampaignEntity campaign2 = createCampaignEntity("campaign-2", "Campaign 2");
        
        when(campaignRepository.findDraftCampaignsReadyToStart(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(campaign1, campaign2));
        
        doThrow(new RuntimeException("Start failed")).when(campaignService).activateCampaign("campaign-1");
        when(campaignService.activateCampaign("campaign-2")).thenReturn(null);
        
        // Should not throw exception, just log and continue
        assertThatCode(() -> schedulerService.autoStartCampaigns())
            .doesNotThrowAnyException();
        
        verify(campaignService).activateCampaign("campaign-1");
        verify(campaignService).activateCampaign("campaign-2");
    }
    
    @Test
    void testAutoStartCampaigns_WhenRepositoryThrowsException_HandlesGracefully() {
        when(campaignRepository.findDraftCampaignsReadyToStart(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        assertThatCode(() -> schedulerService.autoStartCampaigns())
            .doesNotThrowAnyException();
        
        verify(campaignRepository).findDraftCampaignsReadyToStart(any(LocalDateTime.class));
        verify(campaignService, never()).activateCampaign(anyString());
    }
    
    @Test
    void testAutoEndCampaigns_WithCampaignsReadyToEnd_EndsThem() {
        CampaignEntity campaign1 = createCampaignEntity("campaign-1", "Campaign 1");
        CampaignEntity campaign2 = createCampaignEntity("campaign-2", "Campaign 2");
        
        when(campaignRepository.findActiveCampaignsReadyToEnd(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(campaign1, campaign2));
        when(campaignService.endCampaign(anyString())).thenReturn(null);
        
        schedulerService.autoEndCampaigns();
        
        verify(campaignRepository).findActiveCampaignsReadyToEnd(any(LocalDateTime.class));
        verify(campaignService).endCampaign("campaign-1");
        verify(campaignService).endCampaign("campaign-2");
    }
    
    @Test
    void testAutoEndCampaigns_WithNoCampaignsReady_DoesNothing() {
        when(campaignRepository.findActiveCampaignsReadyToEnd(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        schedulerService.autoEndCampaigns();
        
        verify(campaignRepository).findActiveCampaignsReadyToEnd(any(LocalDateTime.class));
        verify(campaignService, never()).endCampaign(anyString());
    }
    
    @Test
    void testAutoStartGames_WithGamesReadyToStart_StartsThem() {
        GameEntity game1 = createGameEntity("game-1", "Game 1");
        GameEntity game2 = createGameEntity("game-2", "Game 2");
        
        when(gameRepository.findDraftGamesReadyToStart(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(game1, game2));
        when(gameService.startGame(anyString())).thenReturn(null);
        
        schedulerService.autoStartGames();
        
        verify(gameRepository).findDraftGamesReadyToStart(any(LocalDateTime.class));
        verify(gameService).startGame("game-1");
        verify(gameService).startGame("game-2");
    }
    
    @Test
    void testAutoStartGames_WithNoGamesReady_DoesNothing() {
        when(gameRepository.findDraftGamesReadyToStart(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        schedulerService.autoStartGames();
        
        verify(gameRepository).findDraftGamesReadyToStart(any(LocalDateTime.class));
        verify(gameService, never()).startGame(anyString());
    }
    
    @Test
    void testAutoStartGames_WhenOneFails_ContinuesWithOthers() {
        GameEntity game1 = createGameEntity("game-1", "Game 1");
        GameEntity game2 = createGameEntity("game-2", "Game 2");
        
        when(gameRepository.findDraftGamesReadyToStart(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(game1, game2));
        
        doThrow(new RuntimeException("Start failed")).when(gameService).startGame("game-1");
        when(gameService.startGame("game-2")).thenReturn(null);
        
        assertThatCode(() -> schedulerService.autoStartGames())
            .doesNotThrowAnyException();
        
        verify(gameService).startGame("game-1");
        verify(gameService).startGame("game-2");
    }
    
    @Test
    void testAutoEndGames_WithGamesReadyToEnd_EndsThem() {
        GameEntity game1 = createGameEntity("game-1", "Game 1");
        GameEntity game2 = createGameEntity("game-2", "Game 2");
        
        when(gameRepository.findActiveGamesReadyToEnd(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(game1, game2));
        when(gameService.stopGame(anyString())).thenReturn(null);
        
        schedulerService.autoEndGames();
        
        verify(gameRepository).findActiveGamesReadyToEnd(any(LocalDateTime.class));
        verify(gameService).stopGame("game-1");
        verify(gameService).stopGame("game-2");
    }
    
    @Test
    void testAutoEndGames_WithNoGamesReady_DoesNothing() {
        when(gameRepository.findActiveGamesReadyToEnd(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        schedulerService.autoEndGames();
        
        verify(gameRepository).findActiveGamesReadyToEnd(any(LocalDateTime.class));
        verify(gameService, never()).stopGame(anyString());
    }
    
    @Test
    void testAutoEndGames_WhenRepositoryThrowsException_HandlesGracefully() {
        when(gameRepository.findActiveGamesReadyToEnd(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        assertThatCode(() -> schedulerService.autoEndGames())
            .doesNotThrowAnyException();
        
        verify(gameRepository).findActiveGamesReadyToEnd(any(LocalDateTime.class));
        verify(gameService, never()).stopGame(anyString());
    }
    
    @Test
    void testAutoStartCampaigns_PassesCurrentDateTime() {
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        
        when(campaignRepository.findDraftCampaignsReadyToStart(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        schedulerService.autoStartCampaigns();
        
        verify(campaignRepository).findDraftCampaignsReadyToStart(timeCaptor.capture());
        
        // Verify that the captured time is close to now (within 1 second)
        LocalDateTime capturedTime = timeCaptor.getValue();
        assertThat(capturedTime).isBetween(
            LocalDateTime.now().minusSeconds(1),
            LocalDateTime.now().plusSeconds(1)
        );
    }
    
    private CampaignEntity createCampaignEntity(String id, String name) {
        CampaignEntity entity = new CampaignEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }
    
    private GameEntity createGameEntity(String id, String name) {
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }
}
