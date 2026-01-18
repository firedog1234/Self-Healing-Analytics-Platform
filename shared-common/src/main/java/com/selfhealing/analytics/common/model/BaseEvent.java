package com.selfhealing.analytics.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class BaseEvent {
    @JsonProperty("event_id")
    private String eventId;
    
    @JsonProperty("event_type")
    private EventType eventType;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("schema_version")
    private String schemaVersion;
    
    @JsonProperty("properties")
    private Map<String, Object> properties;
}
