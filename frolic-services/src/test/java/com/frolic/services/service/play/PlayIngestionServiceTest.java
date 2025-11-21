package com.frolic.services.service.play;

import com.frolic.core.cache.store.RedisResultStore;
import com.frolic.core.common.dto.PlayResultDto;
import com.frolic.core.common.enums.CampaignStatus;
import com.frolic.core.common.enums.GameStatus;
import com.frolic.core.common.enums.PlayStatus;
import com.frolic.core.common.exception.InvalidRequestException;
import com.frolic.core.common.exception.ResourceNotFoundException;
import com.frolic.core.messaging.producer.PlayEventProducer;
import com.frolic.core.repository.entity.CampaignEntity;
import com.frolic.core.repository.entity.GameEntity;
import com.frolic.core.repository.jpa.CampaignRepository;
import com.frolic.core.repository.jpa.GameRepository;
import com.frolic.services.controller.play.request.PlayRequest;
import com.frolic.services.controller.play.response.PlayResponse;
import com.frolic.services.service.admin.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayIngestionService
 */
@ExtendWith(MockitoExtension.class)
class PlayIngestionServiceTest {
    
    @Mock
    private PlayEventProducer playEventProducer;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private RedisResultStore redisResultStore;
    
    private PlayIngestionService playIngestionService;
    
    @BeforeEach
    void setUp() {
        playIngestionService = new PlayIngestionService(
            playEventProducer,
            gameRepository,
            campaignRepository,
            userService,
            redisResultStore
        );
    }
    
    @Test
    void testSubmitPlay_Success_ReturnsResponse() {
        PlayRequest request = new PlayRequest();
        request.setUserId("user-123");
        request.setGameId("game-456");
        
        GameEntity game = new GameEntity();
        game.setId("game-456");
        game.setCampaignId("campaign-789");
        game.setStatus(GameStatus.ACTIVE);
        game.setStartTime(LocalDateTime.now().minusHours(1));
        game.setEndTime(LocalDateTime.now().plusHours(1));
        
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("campaign-789");
        campaign.setStatus(CampaignStatus.ACTIVE);
        
        when(userService.isUserValid("user-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(campaignRepository.findById("campaign-789")).thenReturn(Optional.of(campaign));
        
        PlayResponse response = playIngestionService.submitPlay(request);
        
        assertNotNull(response);
        assertNotNull(response.getPlayId());
        assertEquals("game-456", response.getGameId());
        assertEquals("user-123", response.getUserId());
        assertEquals(PlayStatus.QUEUED, response.getStatus());
        
        verify(playEventProducer, times(1)).publishPlayEvent(any());
    }
    
    @Test
    void testSubmitPlay_InvalidUser_ThrowsException() {
        PlayRequest request = new PlayRequest();
        request.setUserId("invalid-user");
        request.setGameId("game-456");
        
        when(userService.isUserValid("invalid-user")).thenReturn(false);
        
        assertThrows(InvalidRequestException.class, () -> 
            playIngestionService.submitPlay(request)
        );
        
        verify(playEventProducer, never()).publishPlayEvent(any());
    }
    
    @Test
    void testSubmitPlay_GameNotFound_ThrowsException() {
        PlayRequest request = new PlayRequest();
        request.setUserId("user-123");
        request.setGameId("nonexistent-game");
        
        when(userService.isUserValid("user-123")).thenReturn(true);
        when(gameRepository.findById("nonexistent-game")).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> 
            playIngestionService.submitPlay(request)
        );
        
        verify(playEventProducer, never()).publishPlayEvent(any());
    }
    
    @Test
    void testSubmitPlay_GameNotActive_ThrowsException() {
        PlayRequest request = new PlayRequest();
        request.setUserId("user-123");
        request.setGameId("game-456");
        
        GameEntity game = new GameEntity();
        game.setId("game-456");
        game.setStatus(GameStatus.ENDED);
        
        when(userService.isUserValid("user-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        
        assertThrows(InvalidRequestException.class, () -> 
            playIngestionService.submitPlay(request)
        );
        
        verify(playEventProducer, never()).publishPlayEvent(any());
    }
    
    @Test
    void testSubmitPlay_CampaignNotActive_ThrowsException() {
        PlayRequest request = new PlayRequest();
        request.setUserId("user-123");
        request.setGameId("game-456");
        
        GameEntity game = new GameEntity();
        game.setId("game-456");
        game.setCampaignId("campaign-789");
        game.setStatus(GameStatus.ACTIVE);
        game.setStartTime(LocalDateTime.now().minusHours(1));
        game.setEndTime(LocalDateTime.now().plusHours(1));
        
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("campaign-789");
        campaign.setStatus(CampaignStatus.ENDED);
        
        when(userService.isUserValid("user-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(campaignRepository.findById("campaign-789")).thenReturn(Optional.of(campaign));
        
        assertThrows(InvalidRequestException.class, () -> 
            playIngestionService.submitPlay(request)
        );
        
        verify(playEventProducer, never()).publishPlayEvent(any());
    }
    
    @Test
    void testSubmitPlay_GameNotStarted_ThrowsException() {
        PlayRequest request = new PlayRequest();
        request.setUserId("user-123");
        request.setGameId("game-456");
        
        GameEntity game = new GameEntity();
        game.setId("game-456");
        game.setCampaignId("campaign-789");
        game.setStatus(GameStatus.ACTIVE);
        game.setStartTime(LocalDateTime.now().plusHours(1)); // Future start time
        game.setEndTime(LocalDateTime.now().plusHours(2));
        
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("campaign-789");
        campaign.setStatus(CampaignStatus.ACTIVE);
        
        when(userService.isUserValid("user-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(campaignRepository.findById("campaign-789")).thenReturn(Optional.of(campaign));
        
        assertThrows(InvalidRequestException.class, () -> 
            playIngestionService.submitPlay(request)
        );
    }
    
    @Test
    void testSubmitPlay_GameEnded_ThrowsException() {
        PlayRequest request = new PlayRequest();
        request.setUserId("user-123");
        request.setGameId("game-456");
        
        GameEntity game = new GameEntity();
        game.setId("game-456");
        game.setCampaignId("campaign-789");
        game.setStatus(GameStatus.ACTIVE);
        game.setStartTime(LocalDateTime.now().minusHours(2));
        game.setEndTime(LocalDateTime.now().minusHours(1)); // Past end time
        
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("campaign-789");
        campaign.setStatus(CampaignStatus.ACTIVE);
        
        when(userService.isUserValid("user-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(campaignRepository.findById("campaign-789")).thenReturn(Optional.of(campaign));
        
        assertThrows(InvalidRequestException.class, () -> 
            playIngestionService.submitPlay(request)
        );
    }
    
    @Test
    void testGetPlayResult_WhenResultExists_ReturnsWinnerResult() {
        String playId = "play-123";
        
        PlayResultDto resultDto = PlayResultDto.builder()
            .playId(playId)
            .gameId("game-456")
            .userId("user-789")
            .winner(true)
            .couponId("coupon-abc")
            .brandId("brand-xyz")
            .brandName("Brand X")
            .timestamp(LocalDateTime.now())
            .message("Congratulations!")
            .build();
        
        when(redisResultStore.getResult(playId)).thenReturn(resultDto);
        
        PlayResponse response = playIngestionService.getPlayResult(playId);
        
        assertNotNull(response);
        assertEquals(playId, response.getPlayId());
        assertEquals("game-456", response.getGameId());
        assertEquals("user-789", response.getUserId());
        assertTrue(response.getWinner());
        assertEquals("coupon-abc", response.getCouponCode());
        assertEquals("Brand X", response.getBrandName());
        assertEquals(PlayStatus.WINNER, response.getStatus());
    }
    
    @Test
    void testGetPlayResult_WhenResultNotYetAvailable_ReturnsProcessing() {
        String playId = "play-456";
        
        when(redisResultStore.getResult(playId)).thenReturn(null);
        
        PlayResponse response = playIngestionService.getPlayResult(playId);
        
        assertNotNull(response);
        assertEquals(playId, response.getPlayId());
        assertEquals(PlayStatus.PROCESSING, response.getStatus());
        assertNull(response.getWinner()); // Winner is null when result not available
        assertTrue(response.getMessage().contains("not yet available"));
    }
    
    @Test
    void testGetPlayResult_LoserResult() {
        String playId = "play-789";
        
        PlayResultDto resultDto = PlayResultDto.builder()
            .playId(playId)
            .gameId("game-101")
            .userId("user-202")
            .winner(false)
            .timestamp(LocalDateTime.now())
            .message("Better luck next time!")
            .build();
        
        when(redisResultStore.getResult(playId)).thenReturn(resultDto);
        
        PlayResponse response = playIngestionService.getPlayResult(playId);
        
        assertNotNull(response);
        assertEquals(playId, response.getPlayId());
        assertFalse(response.getWinner());
        assertEquals(PlayStatus.LOSER, response.getStatus());
        assertNull(response.getCouponCode());
    }
    
    @Test
    void testSubmitPlay_WithMetadata() {
        PlayRequest request = new PlayRequest();
        request.setUserId("user-123");
        request.setGameId("game-456");
        request.setMetadata(java.util.Map.of("ip", "127.0.0.1"));
        
        GameEntity game = new GameEntity();
        game.setId("game-456");
        game.setCampaignId("campaign-789");
        game.setStatus(GameStatus.ACTIVE);
        game.setStartTime(LocalDateTime.now().minusHours(1));
        game.setEndTime(LocalDateTime.now().plusHours(1));
        
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("campaign-789");
        campaign.setStatus(CampaignStatus.ACTIVE);
        
        when(userService.isUserValid("user-123")).thenReturn(true);
        when(gameRepository.findById("game-456")).thenReturn(Optional.of(game));
        when(campaignRepository.findById("campaign-789")).thenReturn(Optional.of(campaign));
        
        PlayResponse response = playIngestionService.submitPlay(request);
        
        assertNotNull(response);
        assertEquals(PlayStatus.QUEUED, response.getStatus());
        verify(playEventProducer).publishPlayEvent(any());
    }
}
