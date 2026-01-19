#!/bin/bash

echo "=== Checking Self-Healing Analytics Platform Status ==="
echo ""

# Check if services are running
echo "1. Service Status:"
docker-compose ps 2>/dev/null || echo "Services not running. Run: docker-compose up -d"

echo ""
echo "2. Recent Event Generation:"
docker-compose logs --tail=10 event-generator 2>/dev/null | grep "Generated" || echo "No recent events (service might not be running)"

echo ""
echo "3. Recent Ingestion Activity:"
docker-compose logs --tail=10 ingestion-service 2>/dev/null | grep -E "Ingested|Consumed|ERROR" || echo "No ingestion activity (service might not be running)"

echo ""
echo "4. Database Event Count:"
docker-compose exec -T postgres psql -U postgres -d analytics -c "SELECT COUNT(*) as total_events FROM raw_events;" 2>/dev/null || echo "Could not query database (service might not be running)"

echo ""
echo "5. Kafka Connection Check:"
docker-compose exec -T kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | head -5 || echo "Kafka not accessible"

echo ""
echo "=== To start everything, run: docker-compose up -d ==="