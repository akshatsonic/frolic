package com.frolic.services.service.websocket;

import com.frolic.core.cache.store.RedisResultStore;
import com.frolic.core.common.dto.PlayResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResultPollingService
 */
@ExtendWith(MockitoExtension.class)
class ResultPollingServiceTest {
    
    @Mock
    private RedisResultStore redisResultStore;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @InjectMocks
    private ResultPollingService resultPollingService;
    
    @BeforeEach
    void setUp() {
        // Set reel duration to 0 seconds for faster tests
        ReflectionTestUtils.setField(resultPollingService, "reelDurationSeconds", 0);
    }
    
    @Test
    void testPollAndPushResult_ResultAvailableImmediately_PushesResult() throws InterruptedException {
        String playId = "play-123";
        PlayResultDto result = PlayResultDto.builder()
            .playId(playId)
            .winner(true)
            .message("You won!")
            .build();
        
        when(redisResultStore.getResult(playId)).thenReturn(result);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(PlayResultDto.class));
        
        resultPollingService.pollAndPushResult(playId);
        
        // Give async task time to complete
        Thread.sleep(100);
        
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PlayResultDto> resultCaptor = ArgumentCaptor.forClass(PlayResultDto.class);
        
        verify(redisResultStore, atLeastOnce()).getResult(playId);
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), resultCaptor.capture());
        
        assertThat(destinationCaptor.getValue()).isEqualTo("/topic/result/" + playId);
        assertThat(resultCaptor.getValue().getPlayId()).isEqualTo(playId);
        assertThat(resultCaptor.getValue().isWinner()).isTrue();
    }
    
    @Test
    void testPollAndPushResult_ResultNotAvailable_PushesTimeoutMessage() throws InterruptedException {
        String playId = "play-456";
        
        when(redisResultStore.getResult(playId)).thenReturn(null);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(PlayResultDto.class));
        
        resultPollingService.pollAndPushResult(playId);
        
        // Give async task time to complete
        Thread.sleep(100);
        
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PlayResultDto> resultCaptor = ArgumentCaptor.forClass(PlayResultDto.class);
        
        verify(redisResultStore, atLeast(5)).getResult(playId); // Should retry 5 times
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), resultCaptor.capture());
        
        assertThat(destinationCaptor.getValue()).isEqualTo("/topic/result/" + playId);
        assertThat(resultCaptor.getValue().getPlayId()).isEqualTo(playId);
        assertThat(resultCaptor.getValue().isWinner()).isFalse();
        assertThat(resultCaptor.getValue().getMessage()).contains("Result not available");
    }
    
    @Test
    void testPollAndPushResult_ResultAvailableOnRetry_PushesResult() throws InterruptedException {
        String playId = "play-789";
        PlayResultDto result = PlayResultDto.builder()
            .playId(playId)
            .winner(false)
            .message("Better luck next time!")
            .build();
        
        // First 2 calls return null, third call returns result
        when(redisResultStore.getResult(playId))
            .thenReturn(null)
            .thenReturn(null)
            .thenReturn(result);
        
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(PlayResultDto.class));
        
        resultPollingService.pollAndPushResult(playId);
        
        // Give async task time to complete
        Thread.sleep(200);
        
        ArgumentCaptor<PlayResultDto> resultCaptor = ArgumentCaptor.forClass(PlayResultDto.class);
        
        verify(redisResultStore, atLeast(3)).getResult(playId);
        verify(messagingTemplate).convertAndSend(eq("/topic/result/" + playId), resultCaptor.capture());
        
        assertThat(resultCaptor.getValue().getPlayId()).isEqualTo(playId);
        assertThat(resultCaptor.getValue().isWinner()).isFalse();
        assertThat(resultCaptor.getValue().getMessage()).isEqualTo("Better luck next time!");
    }
    
    @Test
    void testPollAndPushResult_ExceptionDuringPolling_HandlesGracefully() throws InterruptedException {
        String playId = "play-error";
        
        when(redisResultStore.getResult(playId)).thenThrow(new RuntimeException("Redis error"));
        
        // Should not throw exception, just log
        assertThatCode(() -> resultPollingService.pollAndPushResult(playId))
            .doesNotThrowAnyException();
        
        // Give async task time to complete
        Thread.sleep(100);
        
        verify(redisResultStore, atLeastOnce()).getResult(playId);
        // Should not send any message on exception
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(PlayResultDto.class));
    }
    
    @Test
    void testPollAndPushResult_WithReelDuration_WaitsBeforePolling() throws InterruptedException {
        // Set reel duration to 1 second
        ReflectionTestUtils.setField(resultPollingService, "reelDurationSeconds", 1);
        
        String playId = "play-reel";
        PlayResultDto result = PlayResultDto.builder()
            .playId(playId)
            .winner(true)
            .build();
        
        when(redisResultStore.getResult(playId)).thenReturn(result);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(PlayResultDto.class));
        
        long startTime = System.currentTimeMillis();
        resultPollingService.pollAndPushResult(playId);
        
        // Give async task time to complete (reel duration + processing)
        Thread.sleep(1200);
        
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        
        // Verify that at least 1 second passed (accounting for some overhead)
        assertThat(elapsedTime).isGreaterThanOrEqualTo(1000);
        
        verify(messagingTemplate).convertAndSend(eq("/topic/result/" + playId), any(PlayResultDto.class));
    }
    
    @Test
    void testPollAndPushResult_DifferentPlayIds_HandlesIndependently() throws InterruptedException {
        String playId1 = "play-1";
        String playId2 = "play-2";
        
        PlayResultDto result1 = PlayResultDto.builder()
            .playId(playId1)
            .winner(true)
            .build();
        
        PlayResultDto result2 = PlayResultDto.builder()
            .playId(playId2)
            .winner(false)
            .build();
        
        when(redisResultStore.getResult(playId1)).thenReturn(result1);
        when(redisResultStore.getResult(playId2)).thenReturn(result2);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(PlayResultDto.class));
        
        resultPollingService.pollAndPushResult(playId1);
        resultPollingService.pollAndPushResult(playId2);
        
        // Give async tasks time to complete
        Thread.sleep(100);
        
        verify(messagingTemplate).convertAndSend(eq("/topic/result/" + playId1), eq(result1));
        verify(messagingTemplate).convertAndSend(eq("/topic/result/" + playId2), eq(result2));
    }
}
