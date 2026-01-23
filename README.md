# Self-Healing Analytics Platform

A Java-based analytics platform that ingests synthetic event data, runs batch analytics pipelines, detects data and pipeline failures, and applies AI Ops techniques to classify, correlate, and explain incidents with recommended remediations.

## What Is This?

A **complete data analytics platform** that:
1. **Generates** synthetic e-commerce events (orders, payments, user actions)
2. **Ingests** events into a database via Kafka
3. **Processes** data through batch analytics pipelines (revenue, funnels, retention)
4. **Monitors** data quality and detects failures automatically
5. **Explains** what went wrong using AI-powered root cause analysis
6. **Suggests** how to fix problems with actionable remediation steps

Think of it as a **smart data platform that watches itself** and automatically diagnoses problems.

## Architecture Overview

The platform consists of 7 microservices working together:

1. **Event Generator** - Produces synthetic e-commerce events (users, orders, payments) with schema versions and intentional corruption for testing
2. **Ingestion Service** - Kafka consumers that normalize events into raw Postgres tables with deduplication and late event handling
3. **Batch Transformation Engine** - Scheduled batch jobs that compute analytics (daily revenue, funnels, retention)
4. **Data Quality & Failure Detection** - Monitors data quality, detects anomalies, and emits structured failure events
5. **Lineage & Dependency Graph** - Neo4j-based service managing pipeline lineage and dependency relationships
6. **AI Ops Engine** - Classifies incidents, correlates failures, generates root cause explanations, and provides remediation recommendations (supports OpenAI/Anthropic or rule-based fallback)
7. **Incident Store & Feedback Loop** - Persists incidents, explanations, and resolutions for continuous learning

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Messaging**: Apache Kafka
- **Databases**: PostgreSQL (analytics data), Neo4j (lineage graph)
- **Containerization**: Docker
- **Orchestration**: Kubernetes with StatefulSets

## Project Structure

```
Self-Healing-Analytics-Platform/
├── event-generator/              # Event Generator microservice
├── ingestion-service/            # Ingestion Service microservice (StatefulSet)
├── batch-transformation-engine/  # Batch Transformation Engine
├── data-quality-service/         # Data Quality & Failure Detection
├── lineage-service/              # Lineage & Dependency Graph
├── ai-ops-engine/                # AI Ops Engine
├── incident-store-service/       # Incident Store & Feedback Loop (StatefulSet)
├── shared-common/                # Shared models and utilities
├── k8s/                          # Kubernetes manifests
├── helm/                         # Helm charts
├── docker-compose.yml            # Local development setup
└── pom.xml                       # Parent Maven POM
```

## Quick Start

### Prerequisites

- **Java 17+** - `java -version` to check
- **Maven 3.8+** - `mvn -version` to check
- **Docker Desktop** - Must be running (green icon in menu bar)
  - Download: https://www.docker.com/products/docker-desktop

### Step-by-Step: How to Run

1. **Build the project:**
   ```bash
   mvn clean package -DskipTests
   ```
   Wait for `BUILD SUCCESS` message.

2. **Build Docker images:**
   ```bash
   docker-compose build
   ```

3. **Start all services:**
   ```bash
   docker-compose up -d
   ```
   Wait 30-60 seconds for all services to start.

4. **Verify everything is running:**
   ```bash
   docker-compose ps
   ```
   All services should show "Up" status.

5. **Check that events are being generated and ingested:**
   ```bash
   # View event generator logs
   docker-compose logs -f event-generator
   
   # View ingestion service logs
   docker-compose logs -f ingestion-service
   
   # Check database for ingested events
   docker-compose exec postgres psql -U postgres -d analytics -c "SELECT COUNT(*) FROM raw_events;"
   ```

6. **Stop all services:**
   ```bash
   docker-compose down
   ```

### What to Expect

- **Event Generator** creates events every 5 seconds
- **Ingestion Service** stores events in PostgreSQL
- **Batch Engine** processes data every minute
- **Data Quality Service** monitors tables every 30 seconds
- **AI Ops Engine** analyzes failures and creates incidents

You should see events appearing in the database within a minute of starting.

### Service Ports

- Event Generator: `8081`
- Ingestion Service: `8082`
- Batch Transformation Engine: `8083`
- Data Quality Service: `8084`
- Lineage Service: `8085`
- AI Ops Engine: `8086`
- Incident Store Service: `8087`
- PostgreSQL: `5432`
- Kafka: `9092`
- Neo4j: `7474` (HTTP), `7687` (Bolt)

### Optional: AI-Powered Diagnoses

The AI Ops Engine supports real AI for intelligent root cause analysis. **No API keys required** - it works fine with rule-based fallback by default.

**To enable AI (optional):**
1. Get an API key from [OpenAI](https://platform.openai.com/api-keys) or [Anthropic](https://console.anthropic.com/)
2. Set environment variables:
   ```bash
   export AI_PROVIDER=openai
   export AI_OPENAI_ENABLED=true
   export AI_OPENAI_API_KEY=sk-your-key-here
   ```
3. Restart: `docker-compose restart ai-ops-engine`

See `ai-ops-engine/AI_SETUP.md` for detailed configuration.

**Cost:** ~$0.0003-0.0005 per incident analysis (only if AI is enabled)

### Kubernetes Deployment (Optional)

For production deployment, see `k8s/` directory for Kubernetes manifests and `helm/` for Helm charts.

## Data Flow

1. **Event Generation** → Event Generator produces synthetic events → Kafka `raw-events` topic
2. **Ingestion** → Ingestion Service consumes from Kafka → Normalizes and stores in PostgreSQL `raw_events` table
3. **Batch Processing** → Batch Transformation Engine reads `raw_events` → Computes analytics → Writes to analytics tables
4. **Data Quality** → Data Quality Service monitors tables → Detects anomalies → Publishes to Kafka `data-quality-checks` topic
5. **AI Ops** → AI Ops Engine consumes quality checks → Classifies and analyzes → Publishes incidents to Kafka `incidents` topic
6. **Incident Storage** → Incident Store Service consumes incidents → Persists to PostgreSQL → Provides feedback loop

## API Endpoints

### Lineage Service (Port 8085)
- `GET /api/lineage/affected/{tableName}` - Get affected tables
- `GET /api/lineage/dependencies/{jobName}` - Get downstream dependencies
- `GET /api/lineage/impact/{tableName}` - Get impact analysis

### Incident Store Service (Port 8087)
- `GET /api/incidents/similar/{classification}` - Get similar historical incidents
- `POST /api/incidents/{incidentId}/resolve` - Resolve an incident

## Troubleshooting

**Docker not starting?**
- Make sure Docker Desktop is running (check menu bar icon)
- Run `docker ps` to verify Docker is accessible

**Services not connecting?**
- Wait 30-60 seconds after `docker-compose up -d` for all services to start
- Check logs: `docker-compose logs <service-name>`
- Verify Kafka is running: `docker-compose ps kafka`

**No events in database?**
- Check event generator logs: `docker-compose logs event-generator`
- Check ingestion service logs: `docker-compose logs ingestion-service`
- Verify Kafka connectivity in logs

**Build errors?**
- Make sure Java 17+ is installed: `java -version`
- Clean and rebuild: `mvn clean package -DskipTests`

## License

This project is for demonstration purposes.

## Contributing

This is a demonstration project for AI Ops and self-healing analytics capabilities.