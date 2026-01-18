package com.selfhealing.analytics.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTransformationService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    @Transactional
    public void computeDailyRevenue() {
        try {
            log.info("Starting daily revenue computation");
            
            LocalDate today = LocalDate.now();
            String dateStr = today.format(DateTimeFormatter.ISO_DATE);
            
            // Initialize table if needed
            initializeAnalyticsTables();
            
            // Compute daily revenue from raw events
            String sql = """
                INSERT INTO analytics_daily_revenue (
                    date, total_revenue, order_count, avg_order_value
                )
                SELECT 
                    DATE(timestamp) as date,
                    COALESCE(SUM((properties_json->>'order_amount')::numeric), 0) as total_revenue,
                    COUNT(*) FILTER (WHERE event_type = 'ORDER_PLACED') as order_count,
                    CASE 
                        WHEN COUNT(*) FILTER (WHERE event_type = 'ORDER_PLACED') > 0 
                        THEN COALESCE(SUM((properties_json->>'order_amount')::numeric), 0) / 
                             COUNT(*) FILTER (WHERE event_type = 'ORDER_PLACED')
                        ELSE 0 
                    END as avg_order_value
                FROM raw_events
                WHERE event_type = 'ORDER_PLACED'
                    AND DATE(timestamp) = ?
                GROUP BY DATE(timestamp)
                ON CONFLICT (date) DO UPDATE SET
                    total_revenue = EXCLUDED.total_revenue,
                    order_count = EXCLUDED.order_count,
                    avg_order_value = EXCLUDED.avg_order_value,
                    updated_at = NOW()
                """;
            
            int rows = jdbcTemplate.update(sql, today);
            log.info("Computed daily revenue for {}: {} rows affected", dateStr, rows);
            
        } catch (Exception e) {
            log.error("Error computing daily revenue", e);
            throw e;
        }
    }
    
    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    @Transactional
    public void computeUserFunnel() {
        try {
            log.info("Starting user funnel computation");
            
            initializeAnalyticsTables();
            
            String sql = """
                INSERT INTO analytics_user_funnel (
                    date, users_created, orders_placed, payments_processed, conversion_rate
                )
                SELECT 
                    DATE(timestamp) as date,
                    COUNT(*) FILTER (WHERE event_type = 'USER_CREATED') as users_created,
                    COUNT(*) FILTER (WHERE event_type = 'ORDER_PLACED') as orders_placed,
                    COUNT(*) FILTER (WHERE event_type = 'PAYMENT_PROCESSED') as payments_processed,
                    CASE 
                        WHEN COUNT(*) FILTER (WHERE event_type = 'USER_CREATED') > 0
                        THEN (COUNT(*) FILTER (WHERE event_type = 'ORDER_PLACED')::numeric / 
                              COUNT(*) FILTER (WHERE event_type = 'USER_CREATED')::numeric) * 100
                        ELSE 0
                    END as conversion_rate
                FROM raw_events
                WHERE DATE(timestamp) = CURRENT_DATE
                GROUP BY DATE(timestamp)
                ON CONFLICT (date) DO UPDATE SET
                    users_created = EXCLUDED.users_created,
                    orders_placed = EXCLUDED.orders_placed,
                    payments_processed = EXCLUDED.payments_processed,
                    conversion_rate = EXCLUDED.conversion_rate,
                    updated_at = NOW()
                """;
            
            int rows = jdbcTemplate.update(sql);
            log.info("Computed user funnel: {} rows affected", rows);
            
        } catch (Exception e) {
            log.error("Error computing user funnel", e);
            throw e;
        }
    }
    
    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    @Transactional
    public void computeUserRetention() {
        try {
            log.info("Starting user retention computation");
            
            initializeAnalyticsTables();
            
            String sql = """
                INSERT INTO analytics_user_retention (
                    cohort_date, user_count, day_1_active, day_7_active, day_30_active
                )
                SELECT 
                    DATE(timestamp) as cohort_date,
                    COUNT(DISTINCT user_id) as user_count,
                    COUNT(DISTINCT user_id) FILTER (
                        WHERE EXISTS (
                            SELECT 1 FROM raw_events re2 
                            WHERE re2.user_id = raw_events.user_id 
                            AND DATE(re2.timestamp) = DATE(raw_events.timestamp) + INTERVAL '1 day'
                        )
                    ) as day_1_active,
                    COUNT(DISTINCT user_id) FILTER (
                        WHERE EXISTS (
                            SELECT 1 FROM raw_events re2 
                            WHERE re2.user_id = raw_events.user_id 
                            AND DATE(re2.timestamp) BETWEEN DATE(raw_events.timestamp) + INTERVAL '1 day'
                            AND DATE(raw_events.timestamp) + INTERVAL '7 days'
                        )
                    ) as day_7_active,
                    COUNT(DISTINCT user_id) FILTER (
                        WHERE EXISTS (
                            SELECT 1 FROM raw_events re2 
                            WHERE re2.user_id = raw_events.user_id 
                            AND DATE(re2.timestamp) BETWEEN DATE(raw_events.timestamp) + INTERVAL '1 day'
                            AND DATE(raw_events.timestamp) + INTERVAL '30 days'
                        )
                    ) as day_30_active
                FROM raw_events
                WHERE event_type = 'USER_CREATED'
                    AND DATE(timestamp) >= CURRENT_DATE - INTERVAL '30 days'
                GROUP BY DATE(timestamp)
                ON CONFLICT (cohort_date) DO UPDATE SET
                    user_count = EXCLUDED.user_count,
                    day_1_active = EXCLUDED.day_1_active,
                    day_7_active = EXCLUDED.day_7_active,
                    day_30_active = EXCLUDED.day_30_active,
                    updated_at = NOW()
                """;
            
            int rows = jdbcTemplate.update(sql);
            log.info("Computed user retention: {} rows affected", rows);
            
        } catch (Exception e) {
            log.error("Error computing user retention", e);
            throw e;
        }
    }
    
    private void initializeAnalyticsTables() {
        // Daily revenue table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS analytics_daily_revenue (
                date DATE PRIMARY KEY,
                total_revenue NUMERIC(15,2),
                order_count INTEGER,
                avg_order_value NUMERIC(15,2),
                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
            """);
        
        // User funnel table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS analytics_user_funnel (
                date DATE PRIMARY KEY,
                users_created INTEGER,
                orders_placed INTEGER,
                payments_processed INTEGER,
                conversion_rate NUMERIC(5,2),
                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
            """);
        
        // User retention table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS analytics_user_retention (
                cohort_date DATE PRIMARY KEY,
                user_count INTEGER,
                day_1_active INTEGER,
                day_7_active INTEGER,
                day_30_active INTEGER,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
            """);
    }
}
