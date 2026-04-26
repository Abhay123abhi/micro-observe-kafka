# Phase 2: ML Anomaly Detection Service - Implementation Guide

## Overview

The ML Anomaly Detection Service is a Python FastAPI application that:
1. Receives alerts from Grafana via webhooks
2. Enriches them with AI-powered anomaly analysis
3. Sends enhanced alerts to Kafka for notification processing

**Location**: `/f/micro_services/ml-anomaly-detector/`

---

## Service Architecture

```
┌─────────────────────────────────────────────────┐
│             Grafana Alerting                     │
│  (Creates alerts on metric thresholds)           │
└────────────────┬────────────────────────────────┘
                 │ POST /webhooks/grafana/alert
                 ↓
┌─────────────────────────────────────────────────┐
│    ML Anomaly Detection Service (NEW!)           │
│                                                   │
│  1. Receive Grafana alert                       │
│  2. Fetch 30min historical metrics              │
│  3. Anomaly Detection: Isolation Forest          │
│  4. Forecasting: Prophet/Trend analysis         │
│  5. Dependency mapping: cascading failures      │
│  6. Enrich with recommendations                 │
│  7. Send to Kafka                               │
└────────────────┬────────────────────────────────┘
                 │ Send enriched alert
                 ↓ (Kafka topic: alerts-ml)
┌─────────────────────────────────────────────────┐
│          Notification Service                    │
│  (Reads alerts-ml topic, sends emails)          │
└─────────────────────────────────────────────────┘
```

---

## File Structure

```
ml-anomaly-detector/
├── app/
│   ├── __init__.py                      # Main FastAPI application
│   ├── config.py                        # Configuration management
│   ├── models/
│   │   ├── __init__.py                  # [Legacy - integrated into file]
│   │   ├── anomaly_detector.py          # Isolation Forest algorithm
│   │   ├── forecaster.py                # Time-series predictions
│   │   └── dependency_mapper.py         # Service dependency graph
│   └── services/
│       ├── __init__.py                  # Prometheus client
│       ├── prometheus_client.py         # Fetch metrics from Prometheus
│       └── kafka_producer.py            # Send alerts to Kafka
├── docker/
│   └── Dockerfile                       # Container image definition
├── requirements.txt                     # Python dependencies
├── README.md                            # Service documentation
├── DOCKER_COMPOSE_SNIPPET.md           # How to run with Docker
└── ML_SERVICE_DESIGN.md                # Detailed design document
```

---

## Key Components

### 1. **Anomaly Detector** (`app/models/anomaly_detector.py`)

**Algorithm**: Isolation Forest

```python
Algorithm Details:
- Unsupervised machine learning
- Isolates anomalies by random feature selection
- Anomalies have short isolation paths (detected quickly)
- Normal points need many features to isolate

Features Used:
1. Error rate deviation from baseline
2. P95 latency deviation
3. P99 latency deviation
4. Request rate change
5. Combined deviation score (weighted)

Score: 0-1 (1 = strong anomaly)
Threshold: 0.6 (configurable)
```

**How It Works**:

```
Input: Metrics over 30 minutes
  ├─ Error rate: [0.1%, 0.1%, 0.1%, 0.12%, 0.15%, ...]
  ├─ Latency P99: [50ms, 51ms, 52ms, 65ms, 78ms, ...]
  └─ Requests/sec: [1000, 1005, 998, 1010, 980, ...]

Extract Features:
  ├─ Error deviation: +40% (from 0.1% to 0.14%)
  ├─ Latency deviation: +45% (from 50ms to 72.5ms avg)
  └─ Request rate deviation: -5%

Isolation Forest:
  ├─ Build decision trees
  ├─ Anomalies: short path to leaf (score high)
  ├─ Normal: long path to leaf (score low)
  └─ Return: Anomaly score 0.75

Interpretation:
  └─ Score 0.75 = HIGH ANOMALY (>0.7)
     └─ Alert escalates as "CRITICAL"
```

### 2. **Forecaster** (`app/models/forecaster.py`)

**Predicts** next 30 minutes of metrics

```python
Inputs:
- Current metric values
- Historical baseline

Process:
1. Determine trend (increasing, decreasing, stable)
2. Apply forecasting model (Prophet-inspired)
3. Calculate confidence intervals

Output:
{
  "trending": "increasing",
  "forecast_30min": {
    "error_rate": "0.18",
    "latency_p99": "95ms"
  },
  "confidence": 0.75
}

Usage:
- If error rate trending UP → "This will worsen"
- If approaching limits → "Scale services now"
```

### 3. **Dependency Mapper** (`app/models/dependency_mapper.py`)

**Predicts cascading failures**

```python
Graph:
api-gateway
  ├─ order-service
  │  ├─ inventory-service
  │  └─ notification-service
  ├─ product-service
  └─ inventory-service

If inventory-service fails:
  └─ Impacts: [order-service, api-gateway]

Detection:
- Real-time: What breaks if X goes down?
- Proactive: Warn dependent services
```

### 4. **Prometheus Client** (`app/services/prometheus_client.py`)

**Queries metrics** from Prometheus

```python
Queries:
- HTTP error rate (5xx errors per minute)
- Response time percentiles (P95, P99)
- Request rate (req/s)
- CPU/Memory (if available)

Time Range:
- Fetches last 30 minutes
- 1-minute resolution
- Calculates baseline (first 10 min)
- Compares to current (last 5 min)
```

### 5. **Kafka Producer** (`app/services/kafka_producer.py`)

**Sends enriched alerts** to Kafka

```python
Topic: alerts-ml

Message Format:
{
  "timestamp": "2026-04-26T10:30:45Z",
  "service": "order-service",
  "anomaly_score": 0.75,
  "severity": "CRITICAL",
  "forecast": {...},
  "impacted_services": ["api-gateway"],
  "recommendations": [
    "Check database connections",
    "Scale Order Service pods"
  ],
  "false_positive_probability": 0.15
}

Subscriber:
- Notification Service reads this topic
- Sends email with enriched context
```

---

## API Endpoints

### GET /health
```bash
curl http://localhost:8000/health

Response:
{
  "status": "healthy",
  "service": "ml-anomaly-detector",
  "version": "1.0.0"
}
```

### GET /ready
```bash
curl http://localhost:8000/ready

Response:
{
  "status": "ready",
  "dependencies": {
    "prometheus": "connected",
    "kafka": "connected",
    "ml_models": "loaded"
  }
}
```

### GET /metrics
```bash
curl http://localhost:8000/metrics

Response: Prometheus format metrics
ml_alerts_processed_total{service="order-service",severity="HIGH"} 5
ml_anomaly_score{service="order-service"} 0.75
ml_processing_seconds_bucket{component="anomaly_detection",le="0.1"} 12
```

### POST /webhooks/grafana/alert
```bash
curl -X POST http://localhost:8000/webhooks/grafana/alert \
  -H "Content-Type: application/json" \
  -d '{
    "status": "firing",
    "alerts": [{
      "status": "firing",
      "labels": {
        "alertname": "HighErrorRate",
        "service": "order-service",
        "severity": "warning"
      },
      "annotations": {
        "summary": "Error rate is 12%",
        "description": "Current error rate is 12%, threshold is 5%"
      }
    }]
  }'

Response:
{
  "status": "processed",
  "alerts_processed": 1,
  "results": [{
    "status": "processed",
    "service": "order-service",
    "anomaly_score": 0.75,
    "severity": "CRITICAL",
    "impacted_services": ["api-gateway"]
  }]
}
```

---

## Configuration

**Environment Variables** (in `app/config.py`):

```python
# Prometheus
PROMETHEUS_URL = os.getenv("PROMETHEUS_URL", "http://localhost:9090")

# Kafka
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_OUTPUT_TOPIC = os.getenv("KAFKA_OUTPUT_TOPIC", "alerts-ml")

# ML Parameters
ANOMALY_THRESHOLD = float(os.getenv("ANOMALY_THRESHOLD", 0.6))
SAMPLING_INTERVAL = int(os.getenv("SAMPLING_INTERVAL", 30))

# Feature Flags
ENABLE_FORECASTING = "true"
ENABLE_DEPENDENCY_ANALYSIS = "true"
ENABLE_CORRELATION = "true"
```

---

## Installation & Running

### 1. Local Development

```bash
cd /f/micro_services/ml-anomaly-detector

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows

# Install dependencies
pip install -r requirements.txt

# Run service
python app.py
```

Service will start at `http://localhost:8000`

### 2. Docker

```bash
# Build
docker build -f docker/Dockerfile -t ml-anomaly-detector:latest .

# Run standalone
docker run -e PROMETHEUS_URL=http://prometheus:9090 \
           -e KAFKA_BOOTSTRAP_SERVERS=broker:29092 \
           -p 8000:8000 \
           ml-anomaly-detector:latest
```

### 3. Docker Compose

Add to `api-gateway/docker-compose.yml`:

```yaml
  ml-anomaly-detector:
    build:
      context: ../ml-anomaly-detector
      dockerfile: docker/Dockerfile
    ports:
      - '8000:8000'
    environment:
      PROMETHEUS_URL: http://prometheus:9090
      KAFKA_BOOTSTRAP_SERVERS: broker:29092
    depends_on:
      - prometheus
      - broker
```

Then run:
```bash
cd api-gateway
docker-compose up ml-anomaly-detector
```

---

## Alert Processing Flow

### Step 1: Receive Grafana Alert

```json
{
  "status": "firing",
  "alerts": [{
    "labels": {
      "alertname": "HighErrorRate",
      "service": "order-service",
      "severity": "warning"
    }
  }]
}
```

### Step 2: Fetch Metrics

```python
# Query Prometheus for last 30 minutes
error_rate = 0.12  # 12%
latency_p99 = 78ms
request_rate = 980 req/s
baseline_error_rate = 0.10  # 10%
```

### Step 3: Anomaly Detection

```python
Features:
- Error deviation: (0.12 - 0.10) / 0.10 = 0.2 (20% increase)
- Latency deviation: +15%
- Combined score: 0.35 (weighted average)

Isolation Forest:
- Anomaly score: 0.72 (ANOMALY!)
- Severity: HIGH (> 0.7)
```

### Step 4: Forecasting

```python
Trend Analysis:
- Error rate increasing
- Latency increasing
- Predict in 30 min: Error 15%, Latency 95ms
- Status: "WILL WORSEN"
```

### Step 5: Dependency Check

```python
order-service failed?
  ├─ Depends on: inventory-service ✓ OK
  └─ Impacted by: api-gateway

Recommendation: "order-service failure will impact API Gateway"
```

### Step 6: Send to Kafka

```json
{
  "timestamp": "2026-04-26T10:30:45Z",
  "service": "order-service",
  "anomaly_score": 0.72,
  "severity": "HIGH",
  "impacted_services": ["api-gateway"],
  "recommendations": [
    "Check Order Service database pool",
    "Verify Kafka producer lag",
    "Scale Order Service if CPU > 80%"
  ]
}
```

### Step 7: Email Sent

Notification Service receives message and sends email with:
- Service name
- Anomaly details
- Forecast
- Recommended actions
- Impact analysis

---

## Testing

### Test Health
```bash
curl -i http://localhost:8000/health
# Expected: HTTP 200
```

### Test Readiness
```bash
curl -i http://localhost:8000/ready
# Expected: HTTP 200 with all dependencies OK
```

### Test Alert Processing
```bash
curl -X POST http://localhost:8000/webhooks/grafana/alert \
  -H "Content-Type: application/json" \
  -d @test-alert.json

# Check response for:
# - status: "processed"
# - anomaly_score: > 0 (detected something)
# - severity: "HIGH" or "CRITICAL"
```

### Test Metrics Export
```bash
curl http://localhost:8000/metrics | grep ml_alerts_processed
# Expected: metric values shown
```

---

## Monitoring the ML Service

### Prometheus Metrics

The service exports these metrics:

```
ml_alerts_processed_total       # Total alerts by service/severity
ml_anomaly_score                # Score distribution
ml_processing_seconds           # Component timing
ml_anomalies_current            # Currently active anomalies
```

### View in Grafana

1. Go to **http://localhost:3000/explore**
2. Select **Prometheus**
3. Query: `ml_alerts_processed_total`
4. See alerts per service and severity

### Logs

Service logs include trace IDs:

```
2026-04-26 10:30:45 [main] INFO [trace123,span456] Processing alert for order-service
2026-04-26 10:30:45 [main] INFO [trace123,span456] Anomaly score: 0.75
2026-04-26 10:30:45 [main] INFO [trace123,span456] Sending to Kafka topic: alerts-ml
```

---

## Performance

- **Alert processing**: <500ms per alert
- **Prometheus query**: 100-300ms
- **ML prediction**: 50-100ms
- **Kafka send**: 10-50ms
- **Total**: ~500-1000ms per alert (non-blocking)

---

## Troubleshooting

### Service won't start

```bash
# Check Python version
python --version  # Should be 3.7+

# Check dependencies
pip list | grep scikit-learn

# Run with verbose logging
LOG_LEVEL=DEBUG python app.py
```

### No alerts received

```bash
# Check Grafana is sending webhooks
# 1. In Grafana: Alerting → Notification Channels
# 2. Add webhook: http://ml-service:8000/webhooks/grafana/alert
# 3. Test: Click "Send Test Notification"

# Check service logs
docker logs ml-anomaly-detector
```

### Prometheus not responding

```bash
# Test Prometheus directly
curl http://prometheus:9090/-/healthy

# Check URL in env var
echo $PROMETHEUS_URL
```

### Kafka connection refused

```bash
# Ensure Kafka is running
docker ps | grep broker

# Check bootstrap servers
echo $KAFKA_BOOTSTRAP_SERVERS
# Should be: broker:29092 (or localhost:9092 if local)
```

---

## Next Steps (Phase 3)

1. **Grafana Integration**: Set up alert rules that trigger webhooks
2. **Notification Service**: Update to handle `alerts-ml` Kafka topic
3. **Dashboards**: Create dashboards showing ML service activity
4. **Testing**: Load test with synthetic alerts
5. **Tuning**: Adjust anomaly threshold based on false positive rate
6. **Production Deployment**: Deploy to Kubernetes cluster

---

## References

- Isolation Forest Algorithm: https://scikit-learn.org/stable/modules/ensemble.html#isolation-forest
- Prophet Forecasting: https://facebook.github.io/prophet/
- Prometheus Query Language: https://prometheus.io/docs/prometheus/latest/querying/basics/
- FastAPI: https://fastapi.tiangolo.com/
- Kafka-Python: https://kafka-python.readthedocs.io/
