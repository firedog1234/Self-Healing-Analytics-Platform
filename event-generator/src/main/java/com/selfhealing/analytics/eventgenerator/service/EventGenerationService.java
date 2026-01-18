package com.selfhealing.analytics.eventgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.analytics.common.model.BaseEvent;
import com.selfhealing.analytics.common.model.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventGenerationService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private int corruptionMode = 0; // 0 = normal, 1 = schema issues, 2 = missing fields
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void generateEvents() {
        try {
            int eventCount = random.nextInt(10) + 5; // 5-15 events per batch
            
            for (int i = 0; i < eventCount; i++) {
                BaseEvent event = createRandomEvent();
                String eventJson = objectMapper.writeValueAsString(event);
                
                kafkaTemplate.send("raw-events", event.getEventId(), eventJson);
                log.debug("Generated event: {}", event.getEventId());
            }
            
            log.info("Generated {} events", eventCount);
            
            // Occasionally introduce corruption for testing
            if (random.nextInt(100) < 5) { // 5% chance
                generateCorruptedEvent();
            }
            
        } catch (Exception e) {
            log.error("Error generating events", e);
        }
    }
    
    private BaseEvent createRandomEvent() {
        BaseEvent event = new BaseEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now().minusSeconds(random.nextInt(3600)));
        event.setUserId("user-" + (random.nextInt(1000) + 1));
        event.setSchemaVersion("1.0");
        
        EventType[] types = EventType.values();
        event.setEventType(types[random.nextInt(types.length)]);
        
        Map<String, Object> properties = new HashMap<>();
        
        switch (event.getEventType()) {
            case USER_CREATED:
                properties.put("email", "user" + random.nextInt(10000) + "@example.com");
                properties.put("country", getRandomCountry());
                break;
            case ORDER_PLACED:
                properties.put("order_amount", Math.round(random.nextDouble() * 1000 * 100.0) / 100.0);
                properties.put("currency", "USD");
                properties.put("items", random.nextInt(10) + 1);
                break;
            case PAYMENT_PROCESSED:
                properties.put("payment_amount", Math.round(random.nextDouble() * 1000 * 100.0) / 100.0);
                properties.put("payment_method", getRandomPaymentMethod());
                properties.put("transaction_id", "txn-" + UUID.randomUUID().toString().substring(0, 8));
                break;
            case PRODUCT_VIEWED:
                properties.put("product_id", "prod-" + (random.nextInt(100) + 1));
                properties.put("category", getRandomCategory());
                break;
        }
        
        event.setProperties(properties);
        return event;
    }
    
    private void generateCorruptedEvent() {
        try {
            // Generate intentional corruption
            String corruptedJson;
            if (corruptionMode % 3 == 0) {
                // Missing required fields
                corruptedJson = "{\"event_id\":\"" + UUID.randomUUID() + "\",\"timestamp\":\"" + Instant.now() + "\"}";
            } else if (corruptionMode % 3 == 1) {
                // Invalid schema version
                BaseEvent event = createRandomEvent();
                event.setSchemaVersion("invalid-version");
                corruptedJson = objectMapper.writeValueAsString(event);
            } else {
                // Invalid JSON
                corruptedJson = "{invalid json " + UUID.randomUUID();
            }
            
            kafkaTemplate.send("raw-events", UUID.randomUUID().toString(), corruptedJson);
            log.warn("Generated corrupted event for testing");
            corruptionMode++;
        } catch (Exception e) {
            log.error("Error generating corrupted event", e);
        }
    }
    
    private String getRandomCountry() {
        String[] countries = {"US", "UK", "CA", "DE", "FR", "JP", "AU"};
        return countries[random.nextInt(countries.length)];
    }
    
    private String getRandomPaymentMethod() {
        String[] methods = {"credit_card", "debit_card", "paypal", "apple_pay", "google_pay"};
        return methods[random.nextInt(methods.length)];
    }
    
    private String getRandomCategory() {
        String[] categories = {"electronics", "clothing", "books", "home", "sports", "toys"};
        return categories[random.nextInt(categories.length)];
    }
}
