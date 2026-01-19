# Hands-On Walkthrough: See the Platform in Action

This guide shows you step-by-step how to run the project and observe the self-healing analytics platform in action.

## Step 1: Start Everything

```bash
# Navigate to project directory
cd <project-root>

# Build the project first
mvn clean package -DskipTests

# Build and start all services
docker-compose build
docker-compose up -d
```

**Wait 30-60 seconds** for all services to start up. Check status:
```bash
docker-compose ps
```

All services should show "Up" status.

## Step 2: Watch Events Being Generated

Open a terminal and watch the Event Generator create events:

```bash
docker-compose logs -f event-generator
```

**What you'll see:**
- Every 5 seconds, events are generated (5-15 events per batch)
- Events like: `USER_CREATED`, `ORDER_PLACED`, `PAYMENT_PROCESSED`, `PRODUCT_VIEWED`
- Occasionally corrupted events for testing (about 5% of the time)

**Look for:**
```
Generated 12 events
Generated event: <uuid>
Generated corrupted event for testing  (when corruption is introduced)
```

## Step 3: Watch Events Being Ingested

Open another terminal and watch the Ingestion Service consume events:

```bash
docker-compose logs -f ingestion-service
```

**What you'll see:**
- Events being consumed from Kafka
- Events being stored in PostgreSQL
- Duplicate events being skipped
- Corrupted events being stored in a separate table

**Look for:**
```
Ingested event: <uuid>
Skipping duplicate event: <uuid>
Failed to parse event JSON (for corrupted events)
```

## Step 4: Check the Database - See Raw Events

Connect to PostgreSQL and see the ingested data:

```bash
docker-compose exec postgres psql -U postgres -d analytics
```

Then run these SQL queries:

```sql
-- Count total raw events
SELECT COUNT(*) as total_events FROM raw_events;

-- See recent events
SELECT event_id, event_type, timestamp, user_id 
FROM raw_events 
ORDER BY ingested_at DESC 
LIMIT 10;

-- See event type distribution
SELECT event_type, COUNT(*) as count 
FROM raw_events 
GROUP BY event_type 
ORDER BY count DESC;

-- See corrupted events (if any)
SELECT COUNT(*) as corrupted_count FROM raw_events_corrupted;

-- Exit PostgreSQL
\q
```

**What you'll see:**
- Growing number of events as time passes
- Different event types (USER_CREATED, ORDER_PLACED, etc.)
- Events with user IDs, timestamps, and properties stored as JSON

## Step 5: Watch Batch Jobs Transform Data

The Batch Transformation Engine runs scheduled jobs. Watch it:

```bash
docker-compose logs -f batch-transformation-engine
```

**What you'll see:**
- Every 15 minutes: "Starting daily revenue computation"
- Every 30 minutes: "Starting user funnel computation"
- Daily at 1 AM: "Starting user retention computation"

**Wait a few minutes, then check analytics tables:**

```bash
docker-compose exec postgres psql -U postgres -d analytics
```

```sql
-- Check daily revenue analytics
SELECT * FROM analytics_daily_revenue ORDER BY date DESC LIMIT 5;

-- Check user funnel analytics
SELECT * FROM analytics_user_funnel ORDER BY date DESC LIMIT 5;

-- Check retention metrics
SELECT * FROM analytics_user_retention ORDER BY cohort_date DESC LIMIT 5;

\q
```

**What you'll see:**
- `analytics_daily_revenue`: total_revenue, order_count, avg_order_value
- `analytics_user_funnel`: users_created, orders_placed, conversion_rate
- `analytics_user_retention`: day_1_active, day_7_active, day_30_active

## Step 6: Watch Data Quality Monitoring

Open another terminal to watch data quality checks:

```bash
docker-compose logs -f data-quality-service
```

**What you'll see:**
- Every minute: Row count anomaly checks
- Every 2 minutes: Null rate checks
- Every 5 minutes: Schema drift checks

**Look for:**
```
Data quality check failed: ROW_COUNT_ANOMALY - raw_events - FAILED
Data quality check failed: NULL_RATE - raw_events - WARNING
```

These failures are published to Kafka topic `data-quality-checks`.

## Step 7: Watch AI Ops Engine Process Incidents

Watch the AI Ops Engine analyze quality issues and create incidents:

```bash
docker-compose logs -f ai-ops-engine
```

**What you'll see:**
- Data quality checks being received
- Incidents being created and classified
- Root cause explanations being generated
- Remediation recommendations

**Look for:**
```
Processing failed data quality check: <check-id>
Emitted incident: <incident-id> - DATA_INGESTION_FAILURE
```

## Step 8: Check Incidents in the Database

See the incidents that were created:

```bash
docker-compose exec postgres psql -U postgres -d analytics
```

```sql
-- View all incidents
SELECT 
    incident_id, 
    severity, 
    status, 
    classification, 
    detected_at,
    root_cause_explanation
FROM incidents 
ORDER BY detected_at DESC 
LIMIT 10;

-- See incident details
SELECT 
    incident_id,
    severity,
    classification,
    affected_components,
    root_cause_explanation,
    recommended_remediations
FROM incidents 
WHERE status = 'OPEN'
ORDER BY detected_at DESC;

-- Count incidents by classification
SELECT classification, COUNT(*) as count 
FROM incidents 
GROUP BY classification;

\q
```

**What you'll see:**
- Incident records with classifications like:
  - `DATA_INGESTION_FAILURE`
  - `DATA_QUALITY_DEGRADATION`
  - `SCHEMA_COMPATIBILITY_ISSUE`
- Root cause explanations
- Recommended remediations
- Affected components

## Step 9: Test the Lineage Service API

The Lineage Service tracks dependencies between tables and jobs:

```bash
# Get tables affected by a specific table
curl http://localhost:8085/api/lineage/affected/raw_events

# Get downstream dependencies for a job
curl http://localhost:8085/api/lineage/dependencies/computeDailyRevenue

# Get impact analysis for a table
curl http://localhost:8085/api/lineage/impact/raw_events
```

**What you'll see:**
JSON responses showing:
- Which tables are affected when `raw_events` has issues
- Which jobs depend on `computeDailyRevenue`
- Full impact analysis of a table failure

## Step 10: Test the Incident Store API

Query similar incidents:

```bash
# Get similar incidents by classification
curl http://localhost:8087/api/incidents/similar/DATA_INGESTION_FAILURE
```

**What you'll see:**
JSON list of similar historical incidents with:
- Root cause explanations
- Recommended remediations
- Detection timestamps

## Step 11: See the Complete Data Flow

### Terminal 1: Watch Event Generation
```bash
docker-compose logs -f event-generator | grep -E "Generated|corrupted"
```

### Terminal 2: Watch Ingestion
```bash
docker-compose logs -f ingestion-service | grep -E "Ingested|duplicate|Failed"
```

### Terminal 3: Watch Data Quality
```bash
docker-compose logs -f data-quality-service | grep -E "failed|WARNING|FAILED"
```

### Terminal 4: Watch AI Ops
```bash
docker-compose logs -f ai-ops-engine | grep -E "incident|classification|Emitted"
```

### Terminal 5: Monitor Database
```bash
watch -n 5 'docker-compose exec -T postgres psql -U postgres -d analytics -c "SELECT COUNT(*) FROM raw_events;"'
```

## Step 12: Introduce Issues to See Self-Healing

The Event Generator automatically introduces corrupted events (~5% of the time), but you can also:

### Check for Corrupted Events
```bash
docker-compose exec postgres psql -U postgres -d analytics -c "SELECT * FROM raw_events_corrupted LIMIT 5;"
```

### Watch How the System Handles Issues
1. Corrupted events → Stored in `raw_events_corrupted` table
2. Data quality checks → Detect anomalies
3. AI Ops Engine → Creates incidents with explanations
4. Incident Store → Persists for learning

## Step 13: View Real-Time Metrics

### Count Events by Type
```bash
docker-compose exec postgres psql -U postgres -d analytics -c "
SELECT event_type, COUNT(*) as count 
FROM raw_events 
GROUP BY event_type;"
```

### Check Analytics Computed
```bash
docker-compose exec postgres psql -U postgres -d analytics -c "
SELECT 
    (SELECT COUNT(*) FROM raw_events) as raw_events,
    (SELECT COUNT(*) FROM analytics_daily_revenue) as revenue_records,
    (SELECT COUNT(*) FROM analytics_user_funnel) as funnel_records,
    (SELECT COUNT(*) FROM incidents) as incidents_count;"
```

## Step 14: Stop Everything

When done exploring:

```bash
# Stop all services
docker-compose down

# Stop and remove all data (clean slate)
docker-compose down -v
```

## Expected Timeline

Here's what happens over time:

**Immediately (0-30 seconds):**
- Services start up
- Event Generator begins creating events
- Ingestion Service starts consuming events

**Within 1 minute:**
- Events are in PostgreSQL `raw_events` table
- Data quality checks start running
- First incidents may be created if issues detected

**Within 15 minutes:**
- First batch job runs (daily revenue computation)
- Analytics tables start populating
- More incidents may be created based on data quality checks

**Within 30 minutes:**
- User funnel computation runs
- More analytics data available
- Multiple incidents may exist for analysis

## Troubleshooting

### If you don't see events:
```bash
# Check if Event Generator is running
docker-compose ps event-generator

# Check Event Generator logs for errors
docker-compose logs event-generator

# Restart if needed
docker-compose restart event-generator
```

### If database queries fail:
```bash
# Wait a bit longer for PostgreSQL to be ready
sleep 10

# Check PostgreSQL is healthy
docker-compose ps postgres

# Check connection
docker-compose exec postgres psql -U postgres -c "\l"
```

### If no incidents are created:
This is normal if data quality checks pass! The system only creates incidents when issues are detected. Wait longer or check logs to see quality checks running.

## What You Should Observe

✅ **Event Generation**: Continuous stream of synthetic e-commerce events  
✅ **Data Ingestion**: Events flowing from Kafka to PostgreSQL  
✅ **Analytics Computation**: Batch jobs transforming raw data into analytics  
✅ **Quality Monitoring**: Continuous checks on data integrity  
✅ **Incident Detection**: Automated detection and classification of issues  
✅ **Root Cause Analysis**: AI-generated explanations of problems  
✅ **Remediation Suggestions**: Actionable recommendations for fixing issues  
✅ **Lineage Tracking**: Understanding dependencies between components  

This demonstrates a complete self-healing analytics platform in action!