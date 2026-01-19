# Quick Start Guide

## Prerequisites

Before running the project, ensure you have:

1. **Java 17+** installed
   ```bash
   java -version
   ```

2. **Maven 3.8+** installed
   ```bash
   mvn -version
   ```

3. **Docker Desktop** installed and running
   - Download from: https://www.docker.com/products/docker-desktop
   - Make sure Docker Desktop is running (green icon in menu bar)

4. **Git** (optional, if cloning the repo)

## Step-by-Step Setup

### Step 1: Verify Prerequisites

```bash
# Check Java version (should be 17 or higher)
java -version

# Check Maven version
mvn -version

# Check Docker is running
docker ps
```

If `docker ps` fails with "Cannot connect to Docker daemon", start Docker Desktop.

### Step 2: Build the Project

Navigate to the project root directory and build all modules:

```bash
cd <project-root>

# Clean and compile all modules
mvn clean compile

# Package all modules (creates JAR files)
mvn clean package -DskipTests
```

**Note**: The `-DskipTests` flag skips tests for faster builds. Remove it if you want to run tests.

### Step 3: Start Docker Services

```bash
# Build Docker images for all services
docker-compose build

# Start all services in detached mode
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Kafka + Zookeeper (port 9092)
- Neo4j (ports 7474, 7687)
- All 7 microservices (ports 8081-8087)

### Step 4: Verify Services are Running

```bash
# Check status of all containers
docker-compose ps

# View logs from all services
docker-compose logs -f

# View logs from a specific service
docker-compose logs -f event-generator
docker-compose logs -f ingestion-service
```

### Step 5: Verify Services are Working

#### Check Event Generation

```bash
# View event generator logs
docker-compose logs -f event-generator | grep "Generated"
```

You should see messages like: `Generated X events`

#### Check Data Ingestion

```bash
# View ingestion service logs
docker-compose logs -f ingestion-service | grep "Ingested"
```

#### Check Database

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d analytics

# Inside psql, check tables
\dt
SELECT COUNT(*) FROM raw_events;
SELECT COUNT(*) FROM analytics_daily_revenue;
\q
```

#### Check Kafka Topics

```bash
# List Kafka topics (if Kafka tools are available)
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Step 6: Access Service Endpoints

Once services are running, you can access:

- **Event Generator**: http://localhost:8081 (no UI, generates events to Kafka)
- **Ingestion Service**: http://localhost:8082 (no UI, processes Kafka messages)
- **Batch Transformation Engine**: http://localhost:8083 (no UI, runs scheduled jobs)
- **Data Quality Service**: http://localhost:8084 (no UI, monitors data quality)
- **Lineage Service**: http://localhost:8085
  - Get affected tables: `GET http://localhost:8085/api/lineage/affected/raw_events`
  - Get dependencies: `GET http://localhost:8085/api/lineage/dependencies/computeDailyRevenue`
  - Get impact analysis: `GET http://localhost:8085/api/lineage/impact/raw_events`
- **AI Ops Engine**: http://localhost:8086 (no UI, processes incidents)
- **Incident Store Service**: http://localhost:8087
  - Get similar incidents: `GET http://localhost:8087/api/incidents/similar/DATA_INGESTION_FAILURE`

#### Test Lineage Service API

```bash
# Get affected tables
curl http://localhost:8085/api/lineage/affected/raw_events

# Get downstream dependencies
curl http://localhost:8085/api/lineage/dependencies/computeDailyRevenue
```

#### Test Incident Store Service API

```bash
# Get similar incidents
curl http://localhost:8087/api/incidents/similar/DATA_INGESTION_FAILURE
```

## Monitoring the Platform

### View Real-time Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f event-generator
docker-compose logs -f ingestion-service
docker-compose logs -f data-quality-service
docker-compose logs -f ai-ops-engine
```

### Check Service Health

```bash
# Check if all containers are running
docker-compose ps

# Check resource usage
docker stats
```

## Stopping the Services

```bash
# Stop all services (keeps containers)
docker-compose stop

# Stop and remove containers (keeps volumes)
docker-compose down

# Stop and remove containers + volumes (clean slate)
docker-compose down -v
```

## Troubleshooting

### Issue: Docker daemon not running
**Solution**: Start Docker Desktop application

### Issue: Port already in use
**Solution**: Check what's using the port and stop it
```bash
# Find process using port 8081
lsof -i :8081

# Kill the process (replace PID)
kill -9 <PID>
```

### Issue: Services failing to start
**Solution**: Check logs for errors
```bash
docker-compose logs <service-name>
```

### Issue: Database connection errors
**Solution**: Wait for PostgreSQL to be fully ready (may take 10-30 seconds)
```bash
# Wait for postgres to be healthy
docker-compose ps postgres
```

### Issue: Kafka connection errors
**Solution**: Wait for Kafka to be fully ready (may take 30-60 seconds)
```bash
# Wait for kafka to be healthy
docker-compose logs kafka | grep "started"
```

## Development Workflow

### Running Individual Services Locally

1. **Start infrastructure only**:
   ```bash
   docker-compose up -d postgres kafka zookeeper neo4j
   ```

2. **Run a service locally**:
   ```bash
   cd event-generator
   mvn spring-boot:run
   ```

### Rebuilding After Code Changes

```bash
# Rebuild specific service
cd event-generator
mvn clean package -DskipTests
docker-compose build event-generator
docker-compose up -d event-generator

# Or rebuild all services
mvn clean package -DskipTests
docker-compose build
docker-compose up -d
```

## Next Steps

- Check the [README.md](README.md) for detailed architecture information
- Check the [DEPLOYMENT.md](DEPLOYMENT.md) for Kubernetes deployment instructions
- Explore the service logs to see events being processed
- Query the database to see ingested events and analytics results

## Quick Reference

```bash
# Build everything
mvn clean package -DskipTests

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Restart a specific service
docker-compose restart event-generator

# Check service status
docker-compose ps
```
