package com.selfhealing.analytics.lineage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LineageService implements CommandLineRunner {
    
    private final Driver neo4jDriver;
    
    @Override
    public void run(String... args) {
        initializeLineageGraph();
    }
    
    public void initializeLineageGraph() {
        try (Session session = neo4jDriver.session()) {
            // Create nodes and relationships for pipeline lineage
            String cypher = """
                MERGE (raw:Table {name: 'raw_events', type: 'raw'})
                MERGE (rev:Table {name: 'analytics_daily_revenue', type: 'analytics'})
                MERGE (funnel:Table {name: 'analytics_user_funnel', type: 'analytics'})
                MERGE (retention:Table {name: 'analytics_user_retention', type: 'analytics'})
                MERGE (batchDaily:Job {name: 'computeDailyRevenue', type: 'batch'})
                MERGE (batchFunnel:Job {name: 'computeUserFunnel', type: 'batch'})
                MERGE (batchRetention:Job {name: 'computeUserRetention', type: 'batch'})
                MERGE (batchDaily)-[:READS_FROM]->(raw)
                MERGE (batchFunnel)-[:READS_FROM]->(raw)
                MERGE (batchRetention)-[:READS_FROM]->(raw)
                MERGE (batchDaily)-[:WRITES_TO]->(rev)
                MERGE (batchFunnel)-[:WRITES_TO]->(funnel)
                MERGE (batchRetention)-[:WRITES_TO]->(retention)
                MERGE (dqRaw:Check {name: 'check_raw_events', type: 'data_quality'})
                MERGE (dqRev:Check {name: 'check_daily_revenue', type: 'data_quality'})
                MERGE (dqRaw)-[:MONITORS]->(raw)
                MERGE (dqRev)-[:MONITORS]->(rev)
                RETURN count(*) as created
                """;
            
            session.run(cypher);
            log.info("Lineage graph initialized");
        } catch (Exception e) {
            log.error("Error initializing lineage graph", e);
        }
    }
    
    public List<Map<String, Object>> getAffectedTables(String tableName) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (source:Table {name: $tableName})
                MATCH path = (source)<-[:WRITES_TO*]-(job:Job)-[:READS_FROM*]->(dep:Table)
                RETURN DISTINCT dep.name as affected_table, 
                       length(path) as depth
                ORDER BY depth
                """;
            
            return session.run(cypher, Map.of("tableName", tableName))
                    .list(record -> Map.of(
                        "affected_table", record.get("affected_table").asString(),
                        "depth", record.get("depth").asInt()
                    ));
        }
    }
    
    public List<Map<String, Object>> getDownstreamDependencies(String jobName) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (job:Job {name: $jobName})-[:WRITES_TO]->(table:Table)
                OPTIONAL MATCH (table)<-[:READS_FROM]-(downstream:Job)
                RETURN DISTINCT table.name as table, 
                       collect(DISTINCT downstream.name) as dependent_jobs
                """;
            
            return session.run(cypher, Map.of("jobName", jobName))
                    .list(record -> Map.of(
                        "table", record.get("table").asString(),
                        "dependent_jobs", record.get("dependent_jobs").asList()
                    ));
        }
    }
    
    public List<Map<String, Object>> getImpactAnalysis(String tableName) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (source:Table {name: $tableName})
                MATCH path = (source)-[:*]->(affected)
                WHERE affected:Table OR affected:Job
                RETURN DISTINCT labels(affected)[0] as type,
                       affected.name as name,
                       length(path) as distance
                ORDER BY distance
                """;
            
            return session.run(cypher, Map.of("tableName", tableName))
                    .list(record -> Map.of(
                        "type", record.get("type").asString(),
                        "name", record.get("name").asString(),
                        "distance", record.get("distance").asInt()
                    ));
        }
    }
}
