package com.selfhealing.analytics.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class DataQualityCheck {
    @JsonProperty("check_id")
    private String checkId;
    
    @JsonProperty("table_name")
    private String tableName;
    
    @JsonProperty("check_type")
    private CheckType checkType;
    
    @JsonProperty("status")
    private CheckStatus status;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("details")
    private Map<String, Object> details;
    
    @JsonProperty("threshold")
    private Double threshold;
    
    @JsonProperty("actual_value")
    private Double actualValue;
}

