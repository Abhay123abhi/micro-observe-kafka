# ML Anomaly Detection Service

AI-powered failure detection for microservices using machine learning.

## Features

- **Anomaly Detection**: Isolation Forest algorithm for real-time anomaly detection
- **Forecasting**: Predict metric trends 30 minutes ahead
- **Dependency Analysis**: Detect cascading failures
- **Correlation Engine**: Link related anomalies
- **Grafana Integration**: Receive alerts via webhooks, send enriched alerts to Kafka

## Quick Start

### Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Run service
python -m uvicorn app:app --reload
# Service available at http://localhost:8000
```

### Docker

```bash
# Build
docker build -f docker/Dockerfile -t ml-anomaly-detector:latest .

# Run
docker run -e PROMETHEUS_URL=http://prometheus:9090 \
           -e KAFKA_BOOTSTRAP_SERVERS=broker:29092 \
           -p 8000:8000 \
           ml-anomaly-detector:latest
```

## Configuration

Environment variables:

```bash
PROMETHEUS_URL=http://localhost:9090              # Prometheus endpoint
KAFKA_BOOTSTRAP_SERVERS=localhost:9092            # Kafka brokers
KAFKA_OUTPUT_TOPIC=alerts-ml                      # Kafka output topic
LOKI_URL=http://localhost:3100                    # Loki endpoint
ANOMALY_THRESHOLD=0.6                             # Anomaly detection threshold
SAMPLING_INTERVAL=30                              # Metrics fetch interval (seconds)
LOG_LEVEL=INFO                                    # Log level
```

## API Endpoints

### Health Check
```bash
GET /health
```

### Readiness Check
```bash
GET /ready
```

### Metrics (Prometheus format)
```bash
GET /metrics
```

### Grafana Webhook Receiver
```bash
POST /webhooks/grafana/alert
Content-Type: application/json

{
  "status": "firing",
  "alerts": [{
    "status": "firing",
    "labels": {
      "alertname": "HighErrorRate",
      "service": "order-service",
      "severity": "warning"
    },
    "annotations": {
      "summary": "Error rate is 12%"
    }
  }]
}
```

## Algorithms

### 1. Isolation Forest
- Detects statistical anomalies
- O(log n) complexity
- Handles high-dimensional data well
- No need for labeled training data

### 2. Time-Series Forecasting
- Predicts metric trends
- Warns of impending failures
- Based on seasonal decomposition

### 3. Service Dependency Graph
- Maps service interactions
- Predicts cascading failures
- Identifies critical services

## Architecture

```
Prometheus → ML Service (Anomaly Detection)
              ↓
           Kafka Topic: alerts-ml
              ↓
        Notification Service → Email
```

## Integration with Grafana

1. In Grafana, create alert rule
2. Configure webhook: `http://ml-anomaly-detector:8000/webhooks/grafana/alert`
3. ML service receives alert, enriches with context
4. Sends to Kafka for notification service to process

## Monitoring

The service exports Prometheus metrics:

```
ml_alerts_processed_total          # Total alerts processed
ml_anomaly_score                   # Anomaly score distribution
ml_processing_seconds              # Processing time
ml_anomalies_current               # Currently active anomalies
```

## Logs

All logs include trace IDs for correlation:

```
2026-04-26 10:30:45 [main] INFO  [trace123,span456] - Processing alert for order-service
```

## Development

### Project Structure

```
ml-anomaly-detector/
├── app/
│   ├── __init__.py              # FastAPI app
│   ├── config.py                # Configuration
│   ├── models/
│   │   ├── anomaly_detector.py   # Isolation Forest
│   │   ├── forecaster.py         # Time-series prediction
│   │   └── dependency_mapper.py  # Service dependencies
│   └── services/
│       ├── prometheus_client.py  # Prometheus queries
│       └── kafka_producer.py     # Kafka producer
├── docker/
│   └── Dockerfile
└── requirements.txt
```

### Testing

```bash
# Health check
curl http://localhost:8000/health

# Readiness
curl http://localhost:8000/ready

# Metrics
curl http://localhost:8000/metrics
```

## License

MIT
