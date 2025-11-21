package com.frolic.core.messaging.producer;

import com.frolic.core.common.constant.KafkaTopics;
import com.frolic.core.common.dto.PlayEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for play events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlayEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Publish play event to Kafka
     * Uses gameId as partition key for ordering
     */
    public void publishPlayEvent(PlayEventDto event) {
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaTopics.PLAY_EVENTS, event.getGameId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Published play event: playId={}, gameId={}, partition={}", 
                    event.getPlayId(), event.getGameId(), result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish play event: playId={}", event.getPlayId(), ex);
            }
        });
    }
}
