# Self-Healing Analytics Platform

A Java-based analytics platform that ingests synthetic event data, runs batch analytics pipelines, detects data and pipeline failures, and applies AI Ops techniques to classify, correlate, and explain incidents with recommended remediations.

## Architecture Overview

The platform consists of 7 microservices working together:

1. **Event Generator** - Produces synthetic e-commerce events (users, orders, payments) with schema versions and intentional corruption for testing
2. **Ingestion Service** - Kafka consumers that normalize events into raw Postgres tables with deduplication and late event handling
3. **Batch Transformation Engine** - Scheduled batch jobs that compute analytics (daily revenue, funnels, retention)
4. **Data Quality & Failure Detection** - Monitors data quality, detects anomalies, and emits structured failure events
5. **Lineage & Dependency Graph** - Neo4j-based service managing pipeline lineage and dependency relationships
6. **AI Ops Engine** - Classifies incidents, correlates failures, generates root cause explanations, and provides remediation recommendations
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

- Java 17+
- Maven 3.8+
- Docker and Docker Compose
- (Optional) Kubernetes cluster and kubectl
- (Optional) Helm 3.x

### Local Development with Docker Compose

1. **Build all microservices:**
   ```bash
   mvn clean install -DskipTests
   ```

2. **Start all services:**
   ```bash
   docker-compose up -d
   ```

3. **Verify services are running:**
   ```bash
   docker-compose ps
   ```

4. **View logs:**
   ```bash
   docker-compose logs -f event-generator
   docker-compose logs -f ingestion-service
   ```

5. **Stop all services:**
   ```bash
   docker-compose down
   ```

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

### Kubernetes Deployment

1. **Create namespace:**
   ```bash
   kubectl apply -f k8s/namespace.yaml
   ```

2. **Deploy infrastructure:**
   ```bash
   kubectl apply -f k8s/configmaps.yaml
   kubectl apply -f k8s/postgres.yaml
   kubectl apply -f k8s/kafka.yaml
   kubectl apply -f k8s/neo4j.yaml
   ```

3. **Deploy microservices:**
   ```bash
   kubectl apply -f k8s/event-generator.yaml
   kubectl apply -f k8s/ingestion-service.yaml      # StatefulSet
   kubectl apply -f k8s/batch-transformation-engine.yaml
   kubectl apply -f k8s/data-quality-service.yaml
   kubectl apply -f k8s/lineage-service.yaml
   kubectl apply -f k8s/ai-ops-engine.yaml
   kubectl apply -f k8s/incident-store-service.yaml  # StatefulSet
   ```

4. **Check deployment status:**
   ```bash
   kubectl get pods -n analytics-platform
   kubectl get statefulsets -n analytics-platform
   ```

### Helm Deployment

1. **Install using Helm:**
   ```bash
   helm install analytics-platform ./helm/analytics-platform \
     --namespace analytics-platform \
     --create-namespace
   ```

2. **Upgrade deployment:**
   ```bash
   helm upgrade analytics-platform ./helm/analytics-platform \
     --namespace analytics-platform
   ```

3. **Uninstall:**
   ```bash
   helm uninstall analytics-platform --namespace analytics-platform
   ```

## StatefulSet Services

The following services use **StatefulSets** in Kubernetes for stable network identities and stateful behavior:

1. **Ingestion Service** - Requires stable Kafka consumer group identities for proper message partitioning
2. **Incident Store Service** - Manages persistent incident data with stable identity
3. **PostgreSQL** - Database with persistent storage
4. **Kafka** - Message broker with persistent state
5. **Neo4j** - Graph database with persistent storage

Other services use **Deployments** as they are stateless.

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

## Configuration

Services are configured via environment variables or Spring Boot `application.yml`:

- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker address
- `DATABASE_URL` - PostgreSQL connection URL
- `DATABASE_USER` - PostgreSQL username
- `DATABASE_PASSWORD` - PostgreSQL password
- `NEO4J_URI` - Neo4j connection URI
- `NEO4J_USERNAME` - Neo4j username
- `NEO4J_PASSWORD` - Neo4j password

## Development

### Building Individual Services

```bash
cd event-generator
mvn clean package
```

### Running Services Locally (without Docker)

1. Start infrastructure:
   - PostgreSQL on `localhost:5432`
   - Kafka on `localhost:9092`
   - Neo4j on `localhost:7687`

2. Run services:
   ```bash
   cd event-generator
   mvn spring-boot:run
   ```

## Monitoring & Observability

- Logs: All services output structured logs
- Metrics: Available via Spring Boot Actuator endpoints (when enabled)
- Health Checks: Built into Docker Compose and Kubernetes deployments

## Testing

Run tests:
```bash
mvn test
```

## Production Considerations

- Replace in-memory deduplication with Redis
- Use external Kafka cluster (managed service or production-grade deployment)
- Implement proper secret management (e.g., Kubernetes Secrets, Vault)
- Add monitoring and alerting (Prometheus, Grafana)
- Enable TLS/SSL for all connections
- Implement proper backup strategies for PostgreSQL and Neo4j
- Use managed database services in production
- Scale services based on load (especially Ingestion Service and Batch Engine)

## License

This project is for demonstration purposes.

## Contributing

This is a demonstration project for AI Ops and self-healing analytics capabilities.