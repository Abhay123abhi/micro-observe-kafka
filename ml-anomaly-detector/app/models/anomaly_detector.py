"""Anomaly Detection Model - Isolation Forest"""

import logging
from typing import Dict, Any

import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

logger = logging.getLogger(__name__)


class AnomalyDetector:
    """ML model for detecting anomalies using Isolation Forest"""

    def __init__(self, contamination: float = 0.1):
        self.contamination = contamination
        self.scaler = StandardScaler()
        self.model = IsolationForest(contamination=contamination, random_state=42)

    def detect(self, metrics_data: Dict[str, Any]) -> float:
        """Detect anomaly score (0-1)"""
        try:
            features = self._extract_features(metrics_data)
            if not features:
                return 0.0

            X = np.array(features).reshape(1, -1)
            X_scaled = self.scaler.fit_transform(X)

            score = self.model.score_samples(X_scaled)[0]
            anomaly_score = 1.0 / (1.0 + np.exp(score))

            return float(anomaly_score)
        except Exception as e:
            logger.error(f"Anomaly detection error: {e}")
            return 0.0

    def _extract_features(self, metrics_data: Dict[str, Any]) -> list:
        """Extract features from metrics"""
        try:
            current = metrics_data.get("current", {})
            baseline = metrics_data.get("baseline", {})

            features = []

            # Error rate deviation
            err_curr = current.get("error_rate", 0)
            err_base = baseline.get("error_rate", 0.001)
            err_dev = (err_curr - err_base) / err_base if err_base > 0 else 0
            features.append(min(max(err_dev, -10), 10))

            # P95 latency deviation
            p95_curr = current.get("response_time_p95", 0)
            p95_base = baseline.get("response_time_p95", 0.001)
            p95_dev = (p95_curr - p95_base) / p95_base if p95_base > 0 else 0
            features.append(min(max(p95_dev, -10), 10))

            # P99 latency deviation
            p99_curr = current.get("response_time_p99", 0)
            p99_base = baseline.get("response_time_p99", 0.001)
            p99_dev = (p99_curr - p99_base) / p99_base if p99_base > 0 else 0
            features.append(min(max(p99_dev, -10), 10))

            # Request rate deviation
            req_curr = current.get("request_rate", 0)
            req_base = baseline.get("request_rate", 0.001)
            req_dev = (req_curr - req_base) / req_base if req_base > 0 else 0
            features.append(min(max(req_dev, -5), 5))

            # Combined score
            combined = (
                abs(err_dev) * 0.4 +
                abs(p99_dev) * 0.3 +
                abs(p95_dev) * 0.2 +
                abs(req_dev) * 0.1
            )
            features.append(combined)

            return features if len(features) == 5 else None

        except Exception as e:
            logger.error(f"Feature extraction error: {e}")
            return None
