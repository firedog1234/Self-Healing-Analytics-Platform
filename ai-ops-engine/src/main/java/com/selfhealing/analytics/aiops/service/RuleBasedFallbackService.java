package com.selfhealing.analytics.aiops.service;

import com.selfhealing.analytics.common.model.CheckType;
import com.selfhealing.analytics.common.model.DataQualityCheck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Rule-based fallback service when AI is not available or configured.
 * Provides deterministic, template-based explanations and recommendations.
 */
@Service
@Slf4j
public class RuleBasedFallbackService implements AIService {
    
    @Override
    public boolean isAvailable() {
        return true; // Always available as fallback
    }
    
    @Override
    public String generateRootCauseExplanation(List<DataQualityCheck> checks, String tableName, String classification) {
        log.debug("Using rule-based fallback for root cause explanation");
        
        CheckType primaryType = checks.get(0).getCheckType();
        
        StringBuilder explanation = new StringBuilder();
        explanation.append("Root cause analysis indicates ");
        
        switch (primaryType) {
            case ROW_COUNT_ANOMALY:
                explanation.append(String.format(
                    "a significant deviation in row count for table '%s'. " +
                    "This suggests either data ingestion pipeline disruption or upstream source issues.",
                    tableName));
                break;
            case NULL_RATE:
                explanation.append(String.format(
                    "elevated null rate violations in table '%s'. " +
                    "This typically indicates schema validation failures or data transformation errors.",
                    tableName));
                break;
            case SCHEMA_DRIFT:
                explanation.append(String.format(
                    "schema version incompatibility detected in table '%s'. " +
                    "Upstream systems may have introduced breaking changes without proper migration.",
                    tableName));
                break;
            case MISSING_PARTITION:
                explanation.append(String.format(
                    "missing partition detected for table '%s'. " +
                    "This indicates batch job execution failure or scheduling issues.",
                    tableName));
                break;
            default:
                explanation.append("a data quality issue requiring investigation.");
        }
        
        return explanation.toString();
    }
    
    @Override
    public List<String> generateRemediations(List<DataQualityCheck> checks, String classification, String rootCauseExplanation) {
        log.debug("Using rule-based fallback for remediation recommendations");
        
        List<String> remediations = new ArrayList<>();
        
        switch (classification) {
            case "DATA_INGESTION_FAILURE":
                remediations.add("Check Kafka consumer lag and ingestion service health");
                remediations.add("Verify upstream event generator is operational");
                remediations.add("Review network connectivity between services");
                break;
            case "DATA_QUALITY_DEGRADATION":
                remediations.add("Examine recent schema changes or migrations");
                remediations.add("Validate data transformation logic in batch jobs");
                remediations.add("Check for null constraint violations in source data");
                break;
            case "SCHEMA_COMPATIBILITY_ISSUE":
                remediations.add("Implement schema versioning and backward compatibility checks");
                remediations.add("Update ingestion service to handle new schema versions");
                remediations.add("Add schema validation layer before data ingestion");
                break;
            case "BATCH_JOB_FAILURE":
                remediations.add("Inspect batch job execution logs for errors");
                remediations.add("Verify database connectivity and resource availability");
                remediations.add("Check for missing partitions or data dependencies");
                break;
            default:
                remediations.add("Review system logs for error patterns");
                remediations.add("Check service health endpoints");
        }
        
        return remediations;
    }
    
    @Override
    public String classifyIncident(List<DataQualityCheck> checks) {
        log.debug("Using rule-based fallback for incident classification");
        
        CheckType primaryType = checks.get(0).getCheckType();
        
        switch (primaryType) {
            case ROW_COUNT_ANOMALY:
                return "DATA_INGESTION_FAILURE";
            case NULL_RATE:
                return "DATA_QUALITY_DEGRADATION";
            case SCHEMA_DRIFT:
                return "SCHEMA_COMPATIBILITY_ISSUE";
            case MISSING_PARTITION:
                return "BATCH_JOB_FAILURE";
            default:
                return "UNKNOWN_DATA_ISSUE";
        }
    }
}
