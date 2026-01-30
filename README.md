# Self-Healing Analytics Platform

Data pipeline that processes events, runs analytics, and automatically figures out when things break.

## What it does

Generates fake e-commerce events, stores them, runs analytics (revenue, funnels, retention), and watches for problems. When something breaks, it tells you what happened and how to fix it.

## Running it

Need Java 17+, Maven, and Docker Desktop running.

```bash
mvn clean package -DskipTests
docker-compose build
docker-compose up -d
```

Wait a minute for everything to start, then check:

```bash
docker-compose ps
docker-compose logs -f event-generator
```

To see if events are making it to the database:
```bash
docker-compose exec postgres psql -U postgres -d analytics -c "SELECT COUNT(*) FROM raw_events;"
```

## The services

7 microservices:
- Event Generator - makes fake events every 5 seconds
- Ingestion Service - stores events in PostgreSQL
- Batch Engine - computes analytics every 15-30 min
- Data Quality Service - checks for problems every 30 seconds
- Lineage Service - tracks table dependencies (Neo4j)
- AI Ops Engine - analyzes problems and explains what went wrong
- Incident Store - saves incidents

Everything uses Kafka to talk to each other. Data is in PostgreSQL, dependency graph in Neo4j.
