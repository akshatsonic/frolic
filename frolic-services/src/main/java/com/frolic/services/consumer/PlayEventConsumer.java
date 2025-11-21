package com.frolic.services.consumer;

import com.frolic.core.common.constant.KafkaTopics;
import com.frolic.core.common.dto.PlayEventDto;
import com.frolic.services.service.allocation.RewardAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for play events
 * Processes play events and allocates rewards
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlayEventConsumer {
    
    private final RewardAllocationService rewardAllocationService;
    
    @KafkaListener(
        topics = KafkaTopics.PLAY_EVENTS,
        groupId = "reward-allocator-group",
        concurrency = "10"
    )
    public void consumePlayEvent(PlayEventDto event) {
        log.info("Consuming play event: playId={}, gameId={}, userId={}", 
            event.getPlayId(), event.getGameId(), event.getUserId());
        
        try {
            rewardAllocationService.processPlayEvent(event);
        } catch (Exception e) {
            log.error("Error processing play event: playId={}", event.getPlayId(), e);
            // In production, this would go to a dead letter queue
        }
    }
}
