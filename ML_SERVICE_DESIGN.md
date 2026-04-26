# ML Anomaly Detection Service
# Python FastAPI Service for detecting service failures using AI/ML

```
mlservices
├── app.py                 # FastAPI application
├── requirements.txt       # Python dependencies
├── config.py             # Configuration
├── models/
│   ├── __init__.py
│   ├── anomaly_detector.py    # Isolation Forest algorithm
│   ├── forecaster.py          # Prophet time-series forecasting
│   └── dependency_mapper.py    # Service dependency graph
├── services/
│   ├── __init__.py
│   ├── prometheus_client.py    # Fetch metrics from Prometheus
│   ├── kafka_producer.py       # Send alerts to Kafka
│   ├── grafana_webhook.py      # Handle Grafana webhooks
│   └── correlation_engine.py   # Correlate failures
├── routes/
│   ├── __init__.py
│   ├── health.py              # Health check endpoints
│   ├── webhooks.py            # Grafana webhook receivers
│   └── metrics.py             # ML service metrics
└── docker/
    ├── Dockerfile
    └── requirements.txt
```

---

## Key Components

### 1. Anomaly Detector (Isolation Forest)
```python
# Detects statistical anomalies in metrics
# Algorithm: Isolation Forest (unsupervised)
# Advantage: Works without labeled data, handles high dimensions

Detection Types:
- CPU spike anomalies
- Memory leak patterns
- Response time outliers
- Error rate anomalies
- Database latency issues
```

### 2. Forecaster (Prophet)
```python
# Predicts future metrics and warns of issues
# Algorithm: Facebook's Prophet (time-series)
# Advantage: Captures seasonality, trends, holidays

Predictions:
- "Memory will exceed 90% in 20 minutes"
- "Error rate trending up (currently 0.5%, projected 5% in 1 hour)"
- "Disk will fill in 2 days"
```

### 3. Service Dependency Mapper
```python
# Tracks which service calls which
# Predicts cascading failures

Example Graph:
API Gateway → Order Service → Inventory Service
API Gateway → Order Service → Notification Service
```

### 4. Correlation Engine
```python
# Links related anomalies
# Example:
# - Inventory service CPU spike
# - Order service latency increase (depends on inventory)
# → Report correlated failure, not two separate alerts
```

---

## API Endpoints

### Webhook Receiver (from Grafana)
```
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
      "summary": "High error rate on order-service",
      "description": "Error rate is 15%"
    }
  }]
}
```

### Health Check
```
GET /health
Response: {"status": "healthy", "version": "1.0"}
```

### Metrics Export
```
GET /metrics
Response: Prometheus-formatted ML service metrics
```

---

## How It Works (Flow)

```
1. Grafana Alert Triggered
   ↓
2. POST /webhooks/grafana/alert
   ↓
3. ML Service Receives Alert
   ↓
4. Fetch Historical Metrics from Prometheus (last 30min)
   - CPU, Memory, Response Time, Error Rate
   ↓
5. Run Anomaly Detection
   - Isolation Forest: Score 0-1 (1=strong anomaly)
   - Prophet: Current vs. Predicted
   ↓
6. Determine Anomaly Severity
   - Not Anomaly: Ignore (likely a false positive from Grafana)
   - Weak Anomaly: Score 0.3-0.6 → Log as INFO
   - Strong Anomaly: Score 0.6-0.9 → Alert WARNING
   - Critical Anomaly: Score >0.9 → Alert CRITICAL
   ↓
7. Check Service Dependencies
   - Will this impact other services?
   - Notify downstream services' teams
   ↓
8. Enrich Alert with Recommendations
   - "Order service latency high - check Inventory connection"
   - "Scale Order service pods to handle spike"
   ↓
9. Send to Kafka Topic: 'alerts-ml'
   {
     "original_alert": {...},
     "anomaly_score": 0.85,
     "severity": "HIGH",
     "impacted_services": ["api-gateway"],
     "forecast": "Will worsen in 10 minutes",
     "recommendation": "Scale pods or check DB connections",
     "trace_correlation": "3 related services affected"
   }
   ↓
10. Notification Service Receives
    ↓
11. Send Email with Enriched Context
```

---

## Installation & Running

### Local Development
```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate      # Windows

# Install dependencies
pip install -r requirements.txt

# Run service
python app.py
# Service runs on http://localhost:8000
```

### Docker Deployment
```bash
docker build -f docker/Dockerfile -t ml-anomaly-detector:latest .
docker run -e PROMETHEUS_URL=http://prometheus:9090 \
           -e KAFKA_BOOTSTRAP_SERVERS=broker:29092 \
           -e LOKI_URL=http://loki:3100 \
           -p 8000:8000 \
           ml-anomaly-detector:latest
```

### Environment Variables
```
PROMETHEUS_URL=http://localhost:9090
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_OUTPUT_TOPIC=alerts-ml
LOKI_URL=http://localhost:3100
LOG_LEVEL=INFO
ANOMALY_THRESHOLD=0.6
SAMPLING_INTERVAL=30s  # How often to pull metrics
```

---

## Sample Alert Processing

### Input (from Grafana)
```json
{
  "status": "firing",
  "alerts": [{
    "labels": {
      "alertname": "HighErrorRate",
      "service": "order-service"
    },
    "annotations": {
      "summary": "Error rate is 12%"
    }
  }]
}
```

### ML Service Processing

**Step 1: Fetch Metrics**
```
Last 30 min error rates:
[0.1, 0.1, 0.1, 0.1, 0.1, 0.12, 0.12, 0.11, 0.15, 0.18]
Current: 0.12 (12%)
Baseline average: 0.11 (11%)
```

**Step 2: Anomaly Detection Score**
```
Isolation Forest Score: 0.72 (ANOMALY)
- Deviation: +1σ from baseline
- Pattern: Trending up
- Severity: MODERATE-HIGH
```

**Step 3: Forecasting**
```
Prophet Forecast for next 30 min:
- Best case: Stabilize at 12%
- Likely: Increase to 18%
- Worst case: Spike to 25%

Recommendation: "Monitor next 5 minutes. If >15%, investigate Order DB connection pool"
```

**Step 4: Dependency Analysis**
```
Order Service Impacted By: [Inventory Service, Notification Service]
Order Service Impacts: [API Gateway]

Check: Is Inventory also showing anomalies? NO
  → Issue not upstream, likely internal to Order Service

Recommendation: Check Order Service:
- Database query performance
- Thread pool utilization
- Kafka producer lag
```

### Output (to Kafka)
```json
{
  "timestamp": "2026-04-26T10:30:45Z",
  "alert_id": "12345",
  "severity": "HIGH",
  "anomaly_score": 0.72,
  "service": "order-service",
  "metric": "http_server_requests_seconds_total{service='order-service',status='5xx'}",
  "current_value": 0.12,
  "baseline_value": 0.11,
  "deviation": 0.01,
  "forecast_30min": "0.18 ± 0.05",
  "impacted_services": ["api-gateway"],
  "root_cause_candidates": [
    "Database connection pool exhaustion",
    "Slow query in order processing",
    "Kafka producer backlog"
  ],
  "recommended_actions": [
    "Check Order Service database connections",
    "Review slow query log",
    "Check Kafka lag for 'order-placed' topic",
    "Scale Order Service replicas if CPU >80%"
  ],
  "trace_ids": ["abc123", "def456"],
  "is_correlated": false,
  "false_positive_probability": 0.15
}
```

---

## Integration with Existing Stack

```
┌─────────────────────────────────────────────────────────┐
│                  Your Microservices                       │
│  [API GW] → [Order] → [Inventory] → [Notification]      │
└──────────────┬──────────────────────────────────────────┘
               │ Metrics scraped every 15s
               ↓
        ┌─────────────────┐
        │  Prometheus     │
        │  (stores 15d)   │
        └────────┬────────┘
                 │ API query every 30s
                 ↓
        ┌──────────────────────────────────────────┐
        │   ML Anomaly Detection Service (NEW!)    │
        │  • Isolation Forest                      │
        │  • Prophet Forecasting                   │
        │  • Dependency Graph Analysis             │
        │  • Correlation Engine                    │
        └────────┬─────────────────────────────────┘
                 │ Enriched alerts
                 ↓
        ┌─────────────────────────────────────┐
        │  Kafka Topic: 'alerts-ml'           │
        │  [Enhanced alert events]            │
        └────────┬────────────────────────────┘
                 │
                 ↓
        ┌──────────────────────────────┐
        │  Notification Service (updated)  │
        │  Handles 'alerts-ml' topic  │
        │  Sends email with details   │
        └──────────────────────────────┘
```

---

## Next Steps (Phase 2-3)

1. **Now**: Create Python service structure
2. **Tomorrow**: Implement anomaly detection algorithms
3. **Day 3**: Set up Grafana webhook integration
4. **Day 4**: Test & validation
5. **Day 5**: Deploy to Docker, integrate with Kafka
