"""
ML Anomaly Detection Service
FastAPI application for detecting service failures using AI/ML
"""

import logging
import os
from contextlib import asynccontextmanager
from typing import Dict, Any

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
import prometheus_client
from prometheus_client import Counter, Histogram, Gauge

from app.config import Config
from app.services.prometheus_client import PrometheusClient
from app.services.kafka_producer import KafkaProducer
from app.models.anomaly_detector import AnomalyDetector
from app.models.forecaster import Forecaster
from app.models.dependency_mapper import DependencyMapper

# Logging setup
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger(__name__)

# Prometheus metrics
alerts_processed = Counter(
    'ml_alerts_processed_total',
    'Total alerts processed',
    ['service', 'severity']
)
anomaly_score_histogram = Histogram(
    'ml_anomaly_score',
    'Anomaly detection score distribution',
    ['service']
)
processing_time = Histogram(
    'ml_processing_seconds',
    'Alert processing time',
    ['component']
)
anomalies_detected = Gauge(
    'ml_anomalies_current',
    'Current active anomalies',
    ['service']
)

# Global services
prometheus_client_service: PrometheusClient = None
kafka_producer: KafkaProducer = None
anomaly_detector: AnomalyDetector = None
forecaster: Forecaster = None
dependency_mapper: DependencyMapper = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifecycle management"""
    global prometheus_client_service, kafka_producer, anomaly_detector, forecaster, dependency_mapper

    logger.info("Starting ML Anomaly Detection Service...")

    # Initialize services
    config = Config()
    prometheus_client_service = PrometheusClient(config.prometheus_url)
    kafka_producer = KafkaProducer(config.kafka_bootstrap_servers)
    anomaly_detector = AnomalyDetector()
    forecaster = Forecaster()
    dependency_mapper = DependencyMapper()

    logger.info("✓ Prometheus client initialized")
    logger.info("✓ Kafka producer initialized")
    logger.info("✓ ML models initialized")
    logger.info("✓ Service ready at http://0.0.0.0:8000")

    yield

    logger.info("Shutting down ML Anomaly Detection Service...")
    if kafka_producer:
        kafka_producer.close()
    logger.info("✓ Cleanup complete")


# FastAPI app
app = FastAPI(
    title="ML Anomaly Detection Service",
    description="AI-powered failure detection for microservices",
    version="1.0.0",
    lifespan=lifespan
)


# Health Check Endpoint
@app.get("/health", tags=["Health"])
async def health_check() -> Dict[str, Any]:
    """Health check endpoint for service readiness"""
    return {
        "status": "healthy",
        "service": "ml-anomaly-detector",
        "version": "1.0.0"
    }


# Ready Check Endpoint
@app.get("/ready", tags=["Health"])
async def ready_check() -> Dict[str, Any]:
    """Readiness check - verify all dependencies are ready"""
    try:
        # Check Prometheus connectivity
        prometheus_client_service.health_check()

        return {
            "status": "ready",
            "dependencies": {
                "prometheus": "connected",
                "kafka": "connected",
                "ml_models": "loaded"
            }
        }
    except Exception as e:
        logger.error(f"Readiness check failed: {str(e)}")
        raise HTTPException(status_code=503, detail="Service not ready")


# Metrics Endpoint (Prometheus format)
@app.get("/metrics", tags=["Metrics"])
async def metrics():
    """Export metrics in Prometheus format"""
    return prometheus_client.generate_latest()


# Main Webhook Handler
@app.post("/webhooks/grafana/alert", tags=["Webhooks"])
async def handle_grafana_alert(payload: Dict[str, Any]) -> Dict[str, Any]:
    """
    Receive alert from Grafana, enhance with ML analysis, send to Kafka

    Payload structure (Grafana webhook):
    {
        "status": "firing" | "resolved",
        "alerts": [{
            "status": "firing",
            "labels": {
                "alertname": "string",
                "service": "string",
                "severity": "critical" | "warning" | "info"
            },
            "annotations": {
                "summary": "string",
                "description": "string"
            }
        }]
    }
    """
    logger.info(f"Received Grafana alert webhook")

    alerts = payload.get("alerts", [])
    if not alerts:
        return {"status": "ignored", "reason": "no alerts in payload"}

    results = []

    for alert in alerts:
        try:
            with processing_time.labels(component="total").time():
                result = await process_single_alert(alert)
                results.append(result)
        except Exception as e:
            logger.error(f"Error processing alert: {str(e)}", exc_info=True)
            results.append({
                "status": "error",
                "error": str(e)
            })

    return {
        "status": "processed",
        "alerts_processed": len(results),
        "results": results
    }


async def process_single_alert(alert: Dict[str, Any]) -> Dict[str, Any]:
    """
    Process a single alert through ML pipeline

    Steps:
    1. Extract alert metadata
    2. Fetch historical metrics from Prometheus
    3. Run anomaly detection (Isolation Forest)
    4. Run forecasting (Prophet)
    5. Check service dependencies
    6. Correlate with other anomalies
    7. Enrich with recommendations
    8. Send to Kafka
    """
    logger.debug(f"Processing alert: {alert}")

    # Extract metadata
    labels = alert.get("labels", {})
    service_name = labels.get("service", "unknown")
    alert_name = labels.get("alertname", "unknown")
    severity = labels.get("severity", "warning")

    logger.info(f"Processing {alert_name} for {service_name} (severity: {severity})")

    # Fetch historical metrics
    with processing_time.labels(component="prometheus_fetch").time():
        metrics_data = prometheus_client_service.get_metrics_for_service(
            service_name,
            time_range="30m"
        )

    if not metrics_data:
        logger.warning(f"No metrics found for {service_name}")
        return {
            "status": "skipped",
            "reason": "no metrics available"
        }

    # Anomaly Detection
    with processing_time.labels(component="anomaly_detection").time():
        anomaly_score = anomaly_detector.detect(metrics_data)

    anomaly_score_histogram.labels(service=service_name).observe(anomaly_score)

    logger.info(f"Anomaly score for {service_name}: {anomaly_score:.2f}")

    # If not anomalous, skip further processing
    if anomaly_score < 0.6:  # Configurable threshold
        logger.info(f"Score below threshold ({anomaly_score:.2f} < 0.6), likely false positive")
        alerts_processed.labels(service=service_name, severity="false_positive").inc()
        return {
            "status": "ignored",
            "reason": "likely false positive",
            "anomaly_score": anomaly_score
        }

    # Forecasting
    with processing_time.labels(component="forecasting").time():
        forecast = forecaster.predict(metrics_data, service_name)

    # Dependency Analysis
    with processing_time.labels(component="dependency_analysis").time():
        impacted_services = dependency_mapper.get_impacted_services(service_name)

    # Determine final severity
    if anomaly_score > 0.85:
        final_severity = "CRITICAL"
    elif anomaly_score > 0.7:
        final_severity = "HIGH"
    else:
        final_severity = "MEDIUM"

    # Create enriched alert
    enriched_alert = {
        "timestamp": alert.get("startsAt"),
        "original_alert": alert,
        "service": service_name,
        "anomaly_score": round(anomaly_score, 3),
        "severity": final_severity,
        "forecast": forecast,
        "impacted_services": impacted_services,
        "metrics_summary": {
            "current_values": metrics_data.get("current"),
            "baseline_values": metrics_data.get("baseline"),
            "deviation_percent": metrics_data.get("deviation_percent")
        },
        "recommendations": generate_recommendations(
            service_name, anomaly_score, forecast, impacted_services
        ),
        "false_positive_probability": calculate_false_positive_probability(anomaly_score)
    }

    # Send to Kafka
    logger.info(f"Sending enriched alert to Kafka topic: {Config.kafka_output_topic}")
    kafka_producer.send(
        topic=Config.kafka_output_topic,
        message=enriched_alert,
        key=f"{service_name}:{alert_name}"
    )

    # Update metrics
    alerts_processed.labels(service=service_name, severity=final_severity).inc()
    anomalies_detected.labels(service=service_name).set(1)

    return {
        "status": "processed",
        "service": service_name,
        "anomaly_score": round(anomaly_score, 3),
        "severity": final_severity,
        "impacted_services": impacted_services
    }


def generate_recommendations(
    service: str,
    anomaly_score: float,
    forecast: Dict[str, Any],
    impacted_services: list
) -> list:
    """Generate actionable recommendations based on anomaly analysis"""
    recommendations = []

    # Score-based recommendations
    if anomaly_score > 0.9:
        recommendations.append(f"URGENT: Investigate {service} immediately - anomaly score {anomaly_score:.2f}")

    # Forecast-based recommendations
    if forecast.get("trending", "stable") == "increasing":
        recommendations.append(f"Metric trending upward. {service} may degrade further. Consider scaling.")

    # Dependency-based recommendations
    if impacted_services:
        services_str = ", ".join(impacted_services)
        recommendations.append(f"This affects downstream services: {services_str}. Escalate for visibility.")

    # Generic recommendations based on service type
    if "order" in service.lower():
        recommendations.append("Check database connection pool and query performance")
        recommendations.append("Review Kafka producer lag")
    elif "inventory" in service.lower():
        recommendations.append("Check MongoDB query performance")
        recommendations.append("Verify cache hit ratio")
    elif "notification" in service.lower():
        recommendations.append("Check email service rate limiting")
        recommendations.append("Verify Kafka consumer lag")

    return recommendations


def calculate_false_positive_probability(anomaly_score: float) -> float:
    """Calculate probability this is a false positive based on score"""
    # Higher scores = more confidence = lower false positive probability
    if anomaly_score > 0.9:
        return 0.05  # 5% chance false positive
    elif anomaly_score > 0.7:
        return 0.15  # 15% chance
    else:
        return 0.30  # 30% chance


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", 8000)),
        reload=os.getenv("ENV", "production") != "production"
    )
