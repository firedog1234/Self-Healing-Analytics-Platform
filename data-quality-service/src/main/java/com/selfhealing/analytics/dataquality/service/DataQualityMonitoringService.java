package com.selfhealing.analytics.dataquality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.analytics.common.model.CheckStatus;
import com.selfhealing.analytics.common.model.CheckType;
import com.selfhealing.analytics.common.model.DataQualityCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityMonitoringService {
    
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private final Map<String, Long> baselineRowCounts = new HashMap<>();
    
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void checkRowCountAnomalies() {
        try {
            log.debug("Running row count anomaly checks");
            
            String[] tables = {"raw_events", "analytics_daily_revenue", 
                             "analytics_user_funnel", "analytics_user_retention"};
            
            for (String table : tables) {
                Long currentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table, Long.class);
                
                if (baselineRowCounts.containsKey(table)) {
                    Long baseline = baselineRowCounts.get(table);
                    double change = ((currentCount - baseline) / (double) baseline) * 100;
                    
                    CheckStatus status;
                    if (Math.abs(change) > 50) { // 50% threshold
                        status = CheckStatus.FAILED;
                    } else if (Math.abs(change) > 25) {
                        status = CheckStatus.WARNING;
                    } else {
                        status = CheckStatus.PASSED;
                    }
                    
                    DataQualityCheck check = new DataQualityCheck();
                    check.setCheckId(UUID.randomUUID().toString());
                    check.setTableName(table);
                    check.setCheckType(CheckType.ROW_COUNT_ANOMALY);
                    check.setStatus(status);
                    check.setTimestamp(Instant.now());
                    check.setThreshold(50.0);
                    check.setActualValue(Math.abs(change));
                    
                    Map<String, Object> details = new HashMap<>();
                    details.put("baseline_count", baseline);
                    details.put("current_count", currentCount);
                    details.put("change_percent", change);
                    check.setDetails(details);
                    
                    emitQualityCheck(check);
                } else {
                    baselineRowCounts.put(table, currentCount);
                }
            }
            
        } catch (Exception e) {
            log.error("Error checking row count anomalies", e);
        }
    }
    
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    @Transactional
    public void checkNullRates() {
        try {
            log.debug("Running null rate checks");
            
            String sql = """
                SELECT 
                    'raw_events' as table_name,
                    COUNT(*) as total_rows,
                    COUNT(*) FILTER (WHERE user_id IS NULL) as null_user_id,
                    COUNT(*) FILTER (WHERE timestamp IS NULL) as null_timestamp,
                    COUNT(*) FILTER (WHERE event_type IS NULL) as null_event_type
                FROM raw_events
                WHERE ingested_at > NOW() - INTERVAL '1 hour'
                """;
            
            jdbcTemplate.query(sql, rs -> {
                long total = rs.getLong("total_rows");
                if (total > 0) {
                    double nullUserIdRate = (rs.getLong("null_user_id") / (double) total) * 100;
                    double nullTimestampRate = (rs.getLong("null_timestamp") / (double) total) * 100;
                    double nullEventTypeRate = (rs.getLong("null_event_type") / (double) total) * 100;
                    
                    checkAndEmitNullRate("raw_events", "user_id", nullUserIdRate);
                    checkAndEmitNullRate("raw_events", "timestamp", nullTimestampRate);
                    checkAndEmitNullRate("raw_events", "event_type", nullEventTypeRate);
                }
            });
            
        } catch (Exception e) {
            log.error("Error checking null rates", e);
        }
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void checkSchemaDrift() {
        try {
            log.debug("Running schema drift checks");
            
            // Check for unexpected schema versions
            String sql = """
                SELECT schema_version, COUNT(*) as count
                FROM raw_events
                WHERE ingested_at > NOW() - INTERVAL '1 hour'
                GROUP BY schema_version
                """;
            
            jdbcTemplate.query(sql, rs -> {
                String schemaVersion = rs.getString("schema_version");
                long count = rs.getLong("count");
                
                // If we see unexpected schema versions, emit warning
                if (!schemaVersion.equals("1.0")) {
                    DataQualityCheck check = new DataQualityCheck();
                    check.setCheckId(UUID.randomUUID().toString());
                    check.setTableName("raw_events");
                    check.setCheckType(CheckType.SCHEMA_DRIFT);
                    check.setStatus(CheckStatus.WARNING);
                    check.setTimestamp(Instant.now());
                    
                    Map<String, Object> details = new HashMap<>();
                    details.put("unexpected_schema_version", schemaVersion);
                    details.put("occurrence_count", count);
                    check.setDetails(details);
                    
                    emitQualityCheck(check);
                }
            });
            
        } catch (Exception e) {
            log.error("Error checking schema drift", e);
        }
    }
    
    private void checkAndEmitNullRate(String tableName, String columnName, double nullRate) {
        CheckStatus status;
        if (nullRate > 10) { // 10% threshold
            status = CheckStatus.FAILED;
        } else if (nullRate > 5) {
            status = CheckStatus.WARNING;
        } else {
            status = CheckStatus.PASSED;
        }
        
        DataQualityCheck check = new DataQualityCheck();
        check.setCheckId(UUID.randomUUID().toString());
        check.setTableName(tableName);
        check.setCheckType(CheckType.NULL_RATE);
        check.setStatus(status);
        check.setTimestamp(Instant.now());
        check.setThreshold(10.0);
        check.setActualValue(nullRate);
        
        Map<String, Object> details = new HashMap<>();
        details.put("column_name", columnName);
        details.put("null_rate_percent", nullRate);
        check.setDetails(details);
        
        emitQualityCheck(check);
    }
    
    private void emitQualityCheck(DataQualityCheck check) {
        try {
            if (check.getStatus() != CheckStatus.PASSED) {
                String checkJson = objectMapper.writeValueAsString(check);
                kafkaTemplate.send("data-quality-checks", check.getCheckId(), checkJson);
                log.warn("Data quality check failed: {} - {} - {}", 
                    check.getCheckType(), check.getTableName(), check.getStatus());
            }
        } catch (Exception e) {
            log.error("Error emitting quality check", e);
        }
    }
}
