package com.frolic.services.consumer;

import com.frolic.core.common.dto.PlayEventDto;
import com.frolic.core.common.enums.PlayStatus;
import com.frolic.services.service.allocation.RewardAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayEventConsumer
 */
@ExtendWith(MockitoExtension.class)
class PlayEventConsumerTest {
    
    @Mock
    private RewardAllocationService rewardAllocationService;
    
    private PlayEventConsumer playEventConsumer;
    
    @BeforeEach
    void setUp() {
        playEventConsumer = new PlayEventConsumer(rewardAllocationService);
    }
    
    @Test
    void testConsumePlayEvent_CallsAllocationService() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-123")
            .gameId("game-456")
            .userId("user-789")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        playEventConsumer.consumePlayEvent(event);
        
        verify(rewardAllocationService, times(1)).processPlayEvent(event);
    }
    
    @Test
    void testConsumePlayEvent_WithMetadata() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-abc")
            .gameId("game-xyz")
            .userId("user-123")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .metadata(java.util.Map.of("ip", "127.0.0.1"))
            .build();
        
        playEventConsumer.consumePlayEvent(event);
        
        verify(rewardAllocationService).processPlayEvent(event);
    }
    
    @Test
    void testConsumePlayEvent_MultipleEvents() {
        PlayEventDto event1 = PlayEventDto.builder()
            .playId("play-1")
            .gameId("game-1")
            .userId("user-1")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        PlayEventDto event2 = PlayEventDto.builder()
            .playId("play-2")
            .gameId("game-2")
            .userId("user-2")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        playEventConsumer.consumePlayEvent(event1);
        playEventConsumer.consumePlayEvent(event2);
        
        verify(rewardAllocationService, times(2)).processPlayEvent(any(PlayEventDto.class));
    }
    
    @Test
    void testConsumePlayEvent_ServiceThrowsException_DoesNotPropagate() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-error")
            .gameId("game-error")
            .userId("user-error")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        doThrow(new RuntimeException("Processing error"))
            .when(rewardAllocationService).processPlayEvent(event);
        
        // Consumer should handle exception gracefully
        playEventConsumer.consumePlayEvent(event);
        
        verify(rewardAllocationService).processPlayEvent(event);
    }
}
