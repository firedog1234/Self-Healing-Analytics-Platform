package com.selfhealing.analytics.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class Incident {
    @JsonProperty("incident_id")
    private String incidentId;
    
    @JsonProperty("severity")
    private IncidentSeverity severity;
    
    @JsonProperty("status")
    private IncidentStatus status;
    
    @JsonProperty("detected_at")
    private Instant detectedAt;
    
    @JsonProperty("resolved_at")
    private Instant resolvedAt;
    
    @JsonProperty("classification")
    private String classification;
    
    @JsonProperty("affected_components")
    private List<String> affectedComponents;
    
    @JsonProperty("root_cause_explanation")
    private String rootCauseExplanation;
    
    @JsonProperty("recommended_remediations")
    private List<String> recommendedRemediations;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}

