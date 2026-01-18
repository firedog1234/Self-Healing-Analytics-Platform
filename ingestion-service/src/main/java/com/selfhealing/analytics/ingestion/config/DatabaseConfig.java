package com.selfhealing.analytics.ingestion.config;

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
        // Create raw_events table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS raw_events (
                event_id VARCHAR(255) PRIMARY KEY,
                event_type VARCHAR(50),
                timestamp TIMESTAMP WITH TIME ZONE,
                user_id VARCHAR(255),
                schema_version VARCHAR(50),
                properties_json JSONB,
                ingested_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
            """);
        
        // Create corrupted events table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS raw_events_corrupted (
                id VARCHAR(255) PRIMARY KEY,
                raw_json TEXT,
                error_message TEXT,
                ingested_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
            """);
        
        // Create indexes
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_raw_events_timestamp 
            ON raw_events(timestamp)
            """);
        
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_raw_events_user_id 
            ON raw_events(user_id)
            """);
        
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_raw_events_type 
            ON raw_events(event_type)
            """);
        
        log.info("Database schema initialized");
    }
}
