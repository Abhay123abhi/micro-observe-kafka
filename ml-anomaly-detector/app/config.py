"""Configuration management for ML Anomaly Detection Service"""

import os
from typing import Optional


class Config:
    """Application configuration"""

    # Service
    SERVICE_NAME = "ml-anomaly-detector"
    SERVICE_PORT = int(os.getenv("PORT", 8000))
    ENV = os.getenv("ENV", "development")

    # Prometheus
    PROMETHEUS_URL = os.getenv("PROMETHEUS_URL", "http://localhost:9090")
    PROMETHEUS_QUERY_TIMEOUT = 30  # seconds

    # Kafka
    KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092").split(",")
    KAFKA_OUTPUT_TOPIC = os.getenv("KAFKA_OUTPUT_TOPIC", "alerts-ml")
    KAFKA_GROUP_ID = "ml-anomaly-detector"

    # Loki (for log correlation)
    LOKI_URL = os.getenv("LOKI_URL", "http://localhost:3100")

    # ML Parameters
    ANOMALY_THRESHOLD = float(os.getenv("ANOMALY_THRESHOLD", 0.6))
    SAMPLING_INTERVAL = int(os.getenv("SAMPLING_INTERVAL", 30))  # seconds
    METRICS_RETENTION_DAYS = int(os.getenv("METRICS_RETENTION_DAYS", 15))

    # Feature flags
    ENABLE_FORECASTING = os.getenv("ENABLE_FORECASTING", "true").lower() == "true"
    ENABLE_DEPENDENCY_ANALYSIS = os.getenv("ENABLE_DEPENDENCY_ANALYSIS", "true").lower() == "true"
    ENABLE_CORRELATION = os.getenv("ENABLE_CORRELATION", "true").lower() == "true"

    # Logging
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

    @classmethod
    def to_dict(cls) -> dict:
        """Export configuration as dictionary"""
        return {
            k: getattr(cls, k)
            for k in dir(cls)
            if not k.startswith("_") and k.isupper()
        }
