package com.frolic.services.service.websocket;

import com.frolic.core.cache.store.RedisResultStore;
import com.frolic.core.common.dto.PlayResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for polling and pushing play results via WebSocket
 * Implements 10-second reel timing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResultPollingService {
    
    private final RedisResultStore redisResultStore;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Value("${frolic.websocket.reel-duration-seconds:10}")
    private int reelDurationSeconds;
    
    /**
     * Poll for result and push to client after reel duration
     * Runs asynchronously on virtual threads
     */
    @Async
    public void pollAndPushResult(String playId) {
        try {
            // Wait for reel duration (10 seconds default)
            log.debug("Waiting {} seconds for reel animation: playId={}", reelDurationSeconds, playId);
            TimeUnit.SECONDS.sleep(reelDurationSeconds);
            
            // Poll for result with retries
            PlayResultDto result = pollWithRetries(playId, 5, 1000);
            
            if (result != null) {
                // Push result to WebSocket topic
                String destination = "/topic/result/" + playId;
                messagingTemplate.convertAndSend(destination, result);
                log.info("Pushed result to WebSocket: playId={}, winner={}", playId, result.isWinner());
            } else {
                // Send timeout message
                PlayResultDto timeoutResult = PlayResultDto.builder()
                    .playId(playId)
                    .winner(false)
                    .message("Result not available. Please try again later.")
                    .build();
                
                String destination = "/topic/result/" + playId;
                messagingTemplate.convertAndSend(destination, timeoutResult);
                log.warn("Result timeout: playId={}", playId);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for result: playId={}", playId, e);
        } catch (Exception e) {
            log.error("Error polling result: playId={}", playId, e);
        }
    }
    
    /**
     * Poll for result with retries
     */
    private PlayResultDto pollWithRetries(String playId, int maxRetries, long retryDelayMs) {
        for (int i = 0; i < maxRetries; i++) {
            PlayResultDto result = redisResultStore.getResult(playId);
            if (result != null) {
                return result;
            }
            
            if (i < maxRetries - 1) {
                try {
                    TimeUnit.MILLISECONDS.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        
        log.warn("Result not found after {} retries: playId={}", maxRetries, playId);
        return null;
    }
}
