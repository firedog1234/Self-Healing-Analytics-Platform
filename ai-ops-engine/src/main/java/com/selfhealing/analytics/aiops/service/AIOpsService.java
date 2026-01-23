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
    private final AIServiceProvider aiServiceProvider;
    private final RuleBasedFallbackService fallbackService;
    
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
        
        // Get AI service (will use best available: OpenAI > Anthropic > Rule-based)
        AIService aiService = aiServiceProvider.getAIService();
        String tableName = checks.get(0).getTableName();
        
        // Classify incident using AI
        String classification = aiService.classifyIncident(checks);
        if (classification == null || classification.isEmpty()) {
            // Fallback if AI classification fails
            classification = getFallbackClassification(checks);
        }
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
        
        // Generate root cause explanation using AI
        String rootCause = aiService.generateRootCauseExplanation(checks, tableName, classification);
        if (rootCause == null || rootCause.isEmpty()) {
            // Fallback to rule-based if AI explanation fails
            log.warn("AI root cause explanation failed, using rule-based fallback");
            rootCause = fallbackService.generateRootCauseExplanation(checks, tableName, classification);
        }
        incident.setRootCauseExplanation(rootCause);
        
        // Generate remediation recommendations using AI
        List<String> remediations = aiService.generateRemediations(checks, classification, rootCause);
        if (remediations == null || remediations.isEmpty()) {
            // Fallback to rule-based if AI remediations fail
            log.warn("AI remediation recommendations failed, using rule-based fallback");
            remediations = fallbackService.generateRemediations(checks, classification, rootCause);
        }
        incident.setRecommendedRemediations(remediations);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("check_count", checks.size());
        metadata.put("first_check_time", checks.get(0).getTimestamp().toString());
        metadata.put("last_check_time", checks.get(checks.size() - 1).getTimestamp().toString());
        metadata.put("ai_provider", aiService.getClass().getSimpleName());
        incident.setMetadata(metadata);
        
        log.info("Created incident {} using AI provider: {}", incident.getIncidentId(), aiService.getClass().getSimpleName());
        
        return incident;
    }
    
    private String getFallbackClassification(List<DataQualityCheck> checks) {
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
