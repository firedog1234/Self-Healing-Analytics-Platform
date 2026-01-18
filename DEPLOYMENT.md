# Deployment Architecture

## StatefulSet vs Deployment Decisions

### Services Using StatefulSets

1. **Ingestion Service** (`ingestion-service`)
   - **Reason**: Requires stable Kafka consumer group identities
   - **Replicas**: 2
   - **Benefits**: 
     - Stable network identity (ingestion-service-0, ingestion-service-1)
     - Predictable Kafka partition assignment
     - Ordered startup/shutdown for consumer group coordination
   - **Storage**: No persistent volumes needed (stateless app, stateful identity)

2. **Incident Store Service** (`incident-store-service`)
   - **Reason**: Manages persistent incident data and provides feedback loop
   - **Replicas**: 1 (can scale if needed)
   - **Benefits**:
     - Stable service identity for incident tracking
     - Consistent data access patterns
   - **Storage**: Uses shared PostgreSQL (no local volumes)

3. **PostgreSQL** (`postgres`)
   - **Reason**: Database with persistent data
   - **Replicas**: 1
   - **Storage**: PersistentVolumeClaim (10Gi)
   - **Benefits**: Data persistence across pod restarts

4. **Kafka** (`kafka`)
   - **Reason**: Message broker with persistent log storage
   - **Replicas**: 1
   - **Storage**: Implicit (via Kafka's internal storage)
   - **Benefits**: Message retention and consumer offset tracking

5. **Neo4j** (`neo4j`)
   - **Reason**: Graph database with persistent data
   - **Replicas**: 1
   - **Storage**: PersistentVolumeClaim (10Gi)
   - **Benefits**: Lineage graph persistence

### Services Using Deployments (Stateless)

1. **Event Generator** - Stateless event producer
2. **Batch Transformation Engine** - Stateless job scheduler (also uses CronJob for scheduled runs)
3. **Data Quality Service** - Stateless monitoring service
4. **Lineage Service** - Stateless API service (connects to Neo4j)
5. **AI Ops Engine** - Stateless incident analysis service

## Kubernetes Resources Summary

| Service | Kind | Replicas | Persistent Storage |
|---------|------|----------|-------------------|
| Event Generator | Deployment | 1 | No |
| Ingestion Service | **StatefulSet** | 2 | No |
| Batch Transformation Engine | Deployment + CronJob | 1 | No |
| Data Quality Service | Deployment | 1 | No |
| Lineage Service | Deployment | 1 | No |
| AI Ops Engine | Deployment | 1 | No |
| Incident Store Service | **StatefulSet** | 1 | No (uses PostgreSQL) |
| PostgreSQL | **StatefulSet** | 1 | Yes (10Gi) |
| Kafka | **StatefulSet** | 1 | Yes (implicit) |
| Neo4j | **StatefulSet** | 1 | Yes (10Gi) |

## Scaling Considerations

### Horizontal Scaling
- **Ingestion Service**: Can scale StatefulSet replicas (2 → N) for higher throughput
- **AI Ops Engine**: Can scale Deployment replicas for parallel incident processing
- **Data Quality Service**: Can scale for concurrent quality checks

### Vertical Scaling
- **Batch Transformation Engine**: May need more memory/CPU for large datasets
- **PostgreSQL**: Scale resources based on data volume
- **Kafka**: Scale based on message throughput

## Networking

All services use Kubernetes Services for discovery:
- **ClusterIP**: Internal service communication
- **Headless Services** (clusterIP: None): For StatefulSets to enable stable DNS

Example DNS names:
- `ingestion-service-0.ingestion-service.analytics-platform.svc.cluster.local`
- `postgres.analytics-platform.svc.cluster.local`

## Health Checks

All services implement health checks:
- **Readiness Probe**: Ensures service is ready to accept traffic
- **Liveness Probe**: Ensures service is still running

## Resource Limits

Default resource allocations:
- **Memory**: 256Mi - 1Gi per service
- **CPU**: 100m - 1000m per service

Adjust based on workload in production.

## Security

- ConfigMaps for non-sensitive configuration
- Secrets for passwords and credentials
- Network policies (can be added for production)
- RBAC (Role-Based Access Control) for service accounts
