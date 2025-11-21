package com.frolic.services.controller.websocket;

import com.frolic.services.service.websocket.ResultPollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for GameWebSocketController
 */
@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {
    
    @Mock
    private ResultPollingService resultPollingService;
    
    @InjectMocks
    private GameWebSocketController gameWebSocketController;
    
    @Test
    void testSubscribeToResult_ValidPlayId_CallsResultPollingService() {
        String playId = "play-123";
        
        doNothing().when(resultPollingService).pollAndPushResult(playId);
        
        gameWebSocketController.subscribeToResult(playId);
        
        verify(resultPollingService).pollAndPushResult(playId);
    }
    
    @Test
    void testSubscribeToResult_DifferentPlayId_CallsResultPollingService() {
        String playId = "play-456";
        
        doNothing().when(resultPollingService).pollAndPushResult(playId);
        
        gameWebSocketController.subscribeToResult(playId);
        
        verify(resultPollingService).pollAndPushResult(playId);
    }
}
