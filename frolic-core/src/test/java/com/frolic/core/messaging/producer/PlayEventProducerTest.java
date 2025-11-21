package com.frolic.core.messaging.producer;

import com.frolic.core.common.constant.KafkaTopics;
import com.frolic.core.common.dto.PlayEventDto;
import com.frolic.core.common.enums.PlayStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayEventProducer
 */
@ExtendWith(MockitoExtension.class)
class PlayEventProducerTest {
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    private PlayEventProducer playEventProducer;
    
    @BeforeEach
    void setUp() {
        playEventProducer = new PlayEventProducer(kafkaTemplate);
    }
    
    @Test
    void testPublishPlayEvent_Success() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-123")
            .gameId("game-456")
            .userId("user-789")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        ProducerRecord<String, Object> producerRecord = 
            new ProducerRecord<>(KafkaTopics.PLAY_EVENTS, "game-456", event);
        RecordMetadata metadata = new RecordMetadata(
            null, 0, 0, 0, 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq(KafkaTopics.PLAY_EVENTS), eq("game-456"), eq(event)))
            .thenReturn(future);
        
        playEventProducer.publishPlayEvent(event);
        
        verify(kafkaTemplate).send(KafkaTopics.PLAY_EVENTS, "game-456", event);
    }
    
    @Test
    void testPublishPlayEvent_UsesGameIdAsPartitionKey() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-abc")
            .gameId("game-xyz")
            .userId("user-123")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        ProducerRecord<String, Object> producerRecord = 
            new ProducerRecord<>(KafkaTopics.PLAY_EVENTS, "game-xyz", event);
        RecordMetadata metadata = new RecordMetadata(null, 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        
        playEventProducer.publishPlayEvent(event);
        
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.PLAY_EVENTS), keyCaptor.capture(), eq(event));
        
        assertEquals("game-xyz", keyCaptor.getValue(), "Should use gameId as partition key");
    }
    
    @Test
    void testPublishPlayEvent_SendsToCorrectTopic() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-1")
            .gameId("game-1")
            .userId("user-1")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        ProducerRecord<String, Object> producerRecord = 
            new ProducerRecord<>(KafkaTopics.PLAY_EVENTS, event);
        RecordMetadata metadata = new RecordMetadata(null, 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        
        playEventProducer.publishPlayEvent(event);
        
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), any());
        
        assertEquals(KafkaTopics.PLAY_EVENTS, topicCaptor.getValue());
    }
    
    @Test
    void testPublishPlayEvent_HandlesFailure() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-fail")
            .gameId("game-fail")
            .userId("user-fail")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .build();
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        
        // Should not throw exception - error is logged in callback
        assertDoesNotThrow(() -> playEventProducer.publishPlayEvent(event));
        
        verify(kafkaTemplate).send(eq(KafkaTopics.PLAY_EVENTS), eq("game-fail"), eq(event));
    }
    
    @Test
    void testPublishPlayEvent_MultipleEvents() {
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
        
        ProducerRecord<String, Object> producerRecord = 
            new ProducerRecord<>(KafkaTopics.PLAY_EVENTS, event1);
        RecordMetadata metadata = new RecordMetadata(null, 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        
        playEventProducer.publishPlayEvent(event1);
        playEventProducer.publishPlayEvent(event2);
        
        verify(kafkaTemplate, times(2)).send(
            eq(KafkaTopics.PLAY_EVENTS), 
            anyString(), 
            any(PlayEventDto.class)
        );
    }
    
    @Test
    void testPublishPlayEvent_WithMetadata() {
        PlayEventDto event = PlayEventDto.builder()
            .playId("play-meta")
            .gameId("game-meta")
            .userId("user-meta")
            .status(PlayStatus.QUEUED)
            .timestamp(LocalDateTime.now())
            .metadata(java.util.Map.of("ip", "127.0.0.1"))
            .build();
        
        ProducerRecord<String, Object> producerRecord = 
            new ProducerRecord<>(KafkaTopics.PLAY_EVENTS, event);
        RecordMetadata metadata = new RecordMetadata(null, 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        
        playEventProducer.publishPlayEvent(event);
        
        verify(kafkaTemplate).send(eq(KafkaTopics.PLAY_EVENTS), eq("game-meta"), eq(event));
    }
}
