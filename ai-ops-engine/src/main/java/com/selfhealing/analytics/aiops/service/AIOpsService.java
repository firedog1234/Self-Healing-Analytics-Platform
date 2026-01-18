package com.selfhealing.analytics.aiops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.analytics.common.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIOpsService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    
    private final Map<String, List<DataQualityCheck>> incidentQueue = new HashMap<>();
    
    @KafkaListener(topics = "data-quality-checks", groupId = "ai-ops-engine-group")
    public void processDataQualityCheck(String checkJson) {
        try {
            DataQualityCheck check = objectMapper.readValue(checkJson, DataQualityCheck.class);
            
            if (check.getStatus() == CheckStatus.FAILED) {
                log.info("Processing failed data quality check: {}", check.getCheckId());
                
                // Group related checks
                String incidentKey = check.getTableName() + "_" + check.getCheckType();
                incidentQueue.computeIfAbsent(incidentKey, k -> new ArrayList<>()).add(check);
                
                // Analyze and create incident
                if (incidentQueue.get(incidentKey).size() >= 3) { // Threshold for creating incident
                    Incident incident = analyzeAndCreateIncident(incidentQueue.remove(incidentKey));
                    emitIncident(incident);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing data quality check", e);
        }
    }
    
    private Incident analyzeAndCreateIncident(List<DataQualityCheck> checks) {
        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID().toString());
        incident.setDetectedAt(Instant.now());
        incident.setStatus(IncidentStatus.OPEN);
        
        // Classify incident
        String classification = classifyIncident(checks);
        incident.setClassification(classification);
        
        // Determine severity
        long criticalCount = checks.stream()
                .filter(c -> c.getActualValue() != null && c.getActualValue() > 50)
                .count();
        incident.setSeverity(criticalCount > 0 ? IncidentSeverity.CRITICAL : IncidentSeverity.HIGH);
        
        // Get affected components
        Set<String> affectedComponents = new HashSet<>();
        checks.forEach(c -> {
            affectedComponents.add(c.getTableName());
            affectedComponents.add(c.getCheckType().name());
        });
        incident.setAffectedComponents(new ArrayList<>(affectedComponents));
        
        // Generate root cause explanation using AI (simplified for demo)
        String rootCause = generateRootCauseExplanation(checks);
        incident.setRootCauseExplanation(rootCause);
        
        // Generate remediation recommendations
        List<String> remediations = generateRemediations(classification, checks);
        incident.setRecommendedRemediations(remediations);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("check_count", checks.size());
        metadata.put("first_check_time", checks.get(0).getTimestamp().toString());
        metadata.put("last_check_time", checks.get(checks.size() - 1).getTimestamp().toString());
        incident.setMetadata(metadata);
        
        return incident;
    }
    
    private String classifyIncident(List<DataQualityCheck> checks) {
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
    
    private String generateRootCauseExplanation(List<DataQualityCheck> checks) {
        // Simplified AI-based explanation (in production, would call LLM API)
        CheckType primaryType = checks.get(0).getCheckType();
        String tableName = checks.get(0).getTableName();
        
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
            default:
                explanation.append("a data quality issue requiring investigation.");
        }
        
        return explanation.toString();
    }
    
    private List<String> generateRemediations(String classification, List<DataQualityCheck> checks) {
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
    
    private void emitIncident(Incident incident) {
        try {
            String incidentJson = objectMapper.writeValueAsString(incident);
            kafkaTemplate.send("incidents", incident.getIncidentId(), incidentJson);
            log.info("Emitted incident: {} - {}", incident.getIncidentId(), incident.getClassification());
        } catch (Exception e) {
            log.error("Error emitting incident", e);
        }
    }
}
