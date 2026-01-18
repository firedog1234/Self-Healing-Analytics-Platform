package com.selfhealing.analytics.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.analytics.common.model.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestionService {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Set<String> processedEventIds = new HashSet<>(); // In-memory dedup (use Redis in production)
    
    @KafkaListener(topics = "raw-events", groupId = "ingestion-service-group")
    @Transactional
    public void consumeEvent(String eventJson, 
                            @Header(KafkaHeaders.RECEIVED_KEY) String eventId,
                            Acknowledgment acknowledgment) {
        try {
            // Deduplication check
            if (processedEventIds.contains(eventId)) {
                log.debug("Skipping duplicate event: {}", eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            BaseEvent event;
            try {
                event = objectMapper.readValue(eventJson, BaseEvent.class);
            } catch (Exception e) {
                log.error("Failed to parse event JSON: {}", eventJson, e);
                // Store corrupted event in dead letter table
                storeCorruptedEvent(eventJson, e.getMessage());
                acknowledgment.acknowledge();
                return;
            }
            
            // Validate required fields
            if (event.getEventId() == null || event.getTimestamp() == null) {
                log.warn("Event missing required fields: {}", eventId);
                storeCorruptedEvent(eventJson, "Missing required fields");
                acknowledgment.acknowledge();
                return;
            }
            
            // Normalize and store in raw table
            storeEvent(event);
            processedEventIds.add(eventId);
            
            // Cleanup old dedup cache entries (keep last 10000)
            if (processedEventIds.size() > 10000) {
                processedEventIds.clear();
            }
            
            acknowledgment.acknowledge();
            log.debug("Ingested event: {}", eventId);
            
        } catch (Exception e) {
            log.error("Error ingesting event: {}", eventId, e);
            // In production, would implement retry logic or DLQ
        }
    }
    
    private void storeEvent(BaseEvent event) {
        String sql = """
            INSERT INTO raw_events (
                event_id, event_type, timestamp, user_id, 
                schema_version, properties_json, ingested_at
            ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;
        
        try {
            String propertiesJson = objectMapper.writeValueAsString(
                event.getProperties() != null ? event.getProperties() : "{}"
            );
            
            jdbcTemplate.update(sql,
                event.getEventId(),
                event.getEventType() != null ? event.getEventType().name() : null,
                event.getTimestamp(),
                event.getUserId(),
                event.getSchemaVersion() != null ? event.getSchemaVersion() : "1.0",
                propertiesJson,
                Instant.now()
            );
        } catch (Exception e) {
            log.error("Error storing event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to store event", e);
        }
    }
    
    private void storeCorruptedEvent(String rawJson, String errorMessage) {
        String sql = """
            INSERT INTO raw_events_corrupted (
                id, raw_json, error_message, ingested_at
            ) VALUES (?, ?, ?, ?)
            """;
        
        try {
            jdbcTemplate.update(sql,
                UUID.randomUUID().toString(),
                rawJson,
                errorMessage,
                Instant.now()
            );
        } catch (Exception e) {
            log.error("Error storing corrupted event", e);
        }
    }
}
