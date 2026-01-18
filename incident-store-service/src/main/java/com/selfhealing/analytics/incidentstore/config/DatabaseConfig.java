package com.selfhealing.analytics.incidentstore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DatabaseConfig implements CommandLineRunner {
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public void run(String... args) {
        initializeSchema();
    }
    
    private void initializeSchema() {
        // Create enum types
        jdbcTemplate.execute("""
            DO $$ BEGIN
                CREATE TYPE incident_severity AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');
            EXCEPTION
                WHEN duplicate_object THEN null;
            END $$;
            """);
        
        jdbcTemplate.execute("""
            DO $$ BEGIN
                CREATE TYPE incident_status AS ENUM ('OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE');
            EXCEPTION
                WHEN duplicate_object THEN null;
            END $$;
            """);
        
        // Create incidents table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS incidents (
                incident_id VARCHAR(255) PRIMARY KEY,
                severity incident_severity,
                status incident_status DEFAULT 'OPEN',
                detected_at TIMESTAMP WITH TIME ZONE,
                resolved_at TIMESTAMP WITH TIME ZONE,
                classification VARCHAR(255),
                affected_components TEXT,
                root_cause_explanation TEXT,
                recommended_remediations TEXT,
                metadata JSONB,
                resolution_notes TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
            """);
        
        // Create indexes
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_incidents_status 
            ON incidents(status)
            """);
        
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_incidents_classification 
            ON incidents(classification)
            """);
        
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_incidents_detected_at 
            ON incidents(detected_at)
            """);
        
        log.info("Incident store schema initialized");
    }
}
