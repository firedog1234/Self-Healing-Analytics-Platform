# How to Run the Project - Step by Step

## Prerequisites Check (One-time setup)

```bash
# Make sure Docker Desktop is running
docker ps

# If error, start Docker Desktop app first
```

---

## Step 1: Build the Project (One terminal)

```bash
# Navigate to project root directory
cd <project-root>
# Or if you're already in the project directory, skip this step

# Build all JAR files
mvn clean package -DskipTests
```

**Wait for this to finish** - you'll see `BUILD SUCCESS` when done.

---

## Step 2: Build Docker Images (Same terminal)

```bash
# Build Docker images for all services
docker-compose build
```

**Wait for this to finish** - can take a few minutes first time.

---

## Step 3: Start All Services (Same terminal)

```bash
# Start everything in background
docker-compose up -d

# Wait 30-60 seconds for services to start
sleep 30
```

---

## Step 4: Check Everything Started (Same terminal)

```bash
# Check all services are running
docker-compose ps

# All should show "Up" status
```

---

## Step 5: Open Multiple Terminals to Watch Logs

### **Terminal 1: Watch Event Generator** (Generates events)
```bash
cd <project-root>
docker-compose logs -f event-generator
```

**What you'll see:**
- `Generated X events` every 5 seconds
- Event IDs being created
- Occasional corrupted events for testing

---

### **Terminal 2: Watch Ingestion Service** (Consumes events from Kafka)
```bash
cd <project-root>
docker-compose logs -f ingestion-service
```

**What you'll see:**
- `Ingested event: <uuid>`
- Events being stored in PostgreSQL

---

### **Terminal 3: Watch Data Quality Service** (Monitors data)
```bash
cd <project-root>
docker-compose logs -f data-quality-service
```

**What you'll see:**
- Data quality checks running
- Warnings/errors when issues detected

---

### **Terminal 4: Watch AI Ops Engine** (Analyzes incidents)
```bash
cd <project-root>
docker-compose logs -f ai-ops-engine
```

**What you'll see:**
- Quality checks being processed
- Incidents being created and classified

---

### **Terminal 5: Query Database** (See the data)

```bash
cd <project-root>

# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d analytics

# Once inside PostgreSQL, run these queries:
```

```sql
-- Count total events
SELECT COUNT(*) FROM raw_events;

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

-- Check analytics tables (after batch jobs run)
SELECT * FROM analytics_daily_revenue ORDER BY date DESC LIMIT 5;

-- Check incidents
SELECT incident_id, severity, classification, detected_at 
FROM incidents 
ORDER BY detected_at DESC 
LIMIT 5;

-- Exit PostgreSQL
\q
```

---

### **Terminal 6: Check Service Status** (Monitor health)

```bash
cd <project-root>

# Check all services
docker-compose ps

# Or watch continuously
watch -n 5 'docker-compose ps'
```

---

## Step 6: Test the APIs (Any terminal)

```bash
# Test Lineage Service
curl http://localhost:8085/api/lineage/affected/raw_events

# Test Incident Store
curl http://localhost:8087/api/incidents/similar/DATA_INGESTION_FAILURE
```

---

## Quick Commands Reference

### See all logs at once:
```bash
docker-compose logs -f
```

### See logs from specific time:
```bash
docker-compose logs --tail=50 event-generator
```

### Restart a service:
```bash
docker-compose restart event-generator
```

### Stop everything:
```bash
docker-compose down
```

### Stop and remove all data (fresh start):
```bash
docker-compose down -v
```

---

## What You Should See (Timeline)

**Immediately (0-30 seconds):**
- Services starting up
- Event Generator begins creating events

**Within 1 minute:**
- Events appearing in PostgreSQL `raw_events` table
- Ingestion Service consuming events

**Within 15 minutes:**
- Batch jobs run (daily revenue computation)
- Analytics tables start populating

**Within 30 minutes:**
- User funnel metrics computed
- Multiple data quality checks run
- Incidents may be created if issues detected

---

## Troubleshooting

### If services won't start:
```bash
# Check what's wrong
docker-compose ps
docker-compose logs <service-name>
```

### If no events in database:
```bash
# Check if ingestion service is running
docker-compose ps ingestion-service

# Check Kafka is working
docker-compose logs kafka | tail -20
```

### If connection errors:
```bash
# Restart Kafka and affected service
docker-compose restart kafka
docker-compose restart ingestion-service
```