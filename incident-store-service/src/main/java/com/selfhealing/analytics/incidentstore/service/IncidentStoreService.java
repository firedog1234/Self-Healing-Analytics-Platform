package com.selfhealing.analytics.incidentstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.analytics.common.model.Incident;
import com.selfhealing.analytics.common.model.IncidentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentStoreService {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "incidents", groupId = "incident-store-group")
    @Transactional
    public void storeIncident(String incidentJson) {
        try {
            Incident incident = objectMapper.readValue(incidentJson, Incident.class);
            
            String sql = """
                INSERT INTO incidents (
                    incident_id, severity, status, detected_at, resolved_at,
                    classification, affected_components, root_cause_explanation,
                    recommended_remediations, metadata, created_at
                ) VALUES (?, ?::incident_severity, ?::incident_status, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (incident_id) DO UPDATE SET
                    severity = EXCLUDED.severity,
                    status = EXCLUDED.status,
                    resolved_at = EXCLUDED.resolved_at,
                    classification = EXCLUDED.classification,
                    affected_components = EXCLUDED.affected_components,
                    root_cause_explanation = EXCLUDED.root_cause_explanation,
                    recommended_remediations = EXCLUDED.recommended_remediations,
                    metadata = EXCLUDED.metadata,
                    updated_at = NOW()
                """;
            
            jdbcTemplate.update(sql,
                incident.getIncidentId(),
                incident.getSeverity() != null ? incident.getSeverity().name() : null,
                incident.getStatus() != null ? incident.getStatus().name() : null,
                incident.getDetectedAt(),
                incident.getResolvedAt(),
                incident.getClassification(),
                incident.getAffectedComponents() != null ? 
                    String.join(",", incident.getAffectedComponents()) : null,
                incident.getRootCauseExplanation(),
                incident.getRecommendedRemediations() != null ?
                    String.join(";", incident.getRecommendedRemediations()) : null,
                incident.getMetadata() != null ? 
                    objectMapper.writeValueAsString(incident.getMetadata()) : "{}",
                Instant.now()
            );
            
            log.info("Stored incident: {}", incident.getIncidentId());
            
        } catch (Exception e) {
            log.error("Error storing incident", e);
        }
    }
    
    public List<Map<String, Object>> getSimilarIncidents(String classification) {
        String sql = """
            SELECT incident_id, severity, status, detected_at, classification,
                   root_cause_explanation, recommended_remediations
            FROM incidents
            WHERE classification = ?
            ORDER BY detected_at DESC
            LIMIT 10
            """;
        
        return jdbcTemplate.query(sql, 
            new Object[]{classification},
            (rs, rowNum) -> {
                Map<String, Object> incident = new HashMap<>();
                incident.put("incident_id", rs.getString("incident_id"));
                incident.put("severity", rs.getString("severity"));
                incident.put("status", rs.getString("status"));
                incident.put("detected_at", rs.getTimestamp("detected_at"));
                incident.put("classification", rs.getString("classification"));
                incident.put("root_cause_explanation", rs.getString("root_cause_explanation"));
                incident.put("recommended_remediations", rs.getString("recommended_remediations"));
                return incident;
            });
    }
    
    public void resolveIncident(String incidentId, String resolution) {
        String sql = """
            UPDATE incidents
            SET status = 'RESOLVED', resolved_at = NOW(), resolution_notes = ?
            WHERE incident_id = ?
            """;
        
        jdbcTemplate.update(sql, resolution, incidentId);
        log.info("Resolved incident: {}", incidentId);
    }
}
