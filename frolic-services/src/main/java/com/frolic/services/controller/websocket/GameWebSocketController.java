package com.frolic.services.controller.websocket;

import com.frolic.services.service.websocket.ResultPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time game result delivery
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {
    
    private final ResultPollingService resultPollingService;
    
    /**
     * Subscribe to play result
     * Client subscribes to /topic/result/{playId}
     * After 10 seconds, result is pushed to subscriber
     */
    @MessageMapping("/subscribe/{playId}")
    public void subscribeToResult(@DestinationVariable String playId) {
        log.info("Client subscribed to result: playId={}", playId);
        
        // Start polling for result (non-blocking with virtual threads)
        resultPollingService.pollAndPushResult(playId);
    }
}
