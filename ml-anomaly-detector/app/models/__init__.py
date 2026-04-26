"""Anomaly Detection Model using Isolation Forest"""

import logging
from typing import Dict, Any

import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

logger = logging.getLogger(__name__)


class AnomalyDetector:
    """ML model for detecting anomalies in metrics using Isolation Forest"""

    def __init__(self, contamination: float = 0.1):
        """
        Initialize anomaly detector

        Args:
            contamination: Expected proportion of anomalies (0-1)
        """
        self.contamination = contamination
        self.scaler = StandardScaler()
        self.model = IsolationForest(
            contamination=contamination,
            random_state=42,
            n_estimators=100
        )
        logger.info(f"✓ Anomaly Detector initialized (contamination={contamination})")

    def detect(self, metrics_data: Dict[str, Any]) -> float:
        """
        Detect anomalies and return anomaly score (0-1)

        Algorithm: Isolation Forest
        - Isolates anomalies by random selection of features and split values
        - Anomalies are few and different → isolated quickly (short path)
        - Normal points require many splittings (long path)

        Args:
            metrics_data: Dictionary with current and baseline metrics

        Returns:
            Anomaly score 0-1 (1 = strong anomaly, 0 = normal)
        """
        try:
            # Extract features
            features = self._extract_features(metrics_data)

            if features is None or len(features) == 0:
                logger.warning("No features extracted for anomaly detection")
                return 0.0

            if len(features) < 2:
                logger.warning("Insufficient features for anomaly detection")
                return 0.0

            # Reshape for sklearn
            X = np.array(features).reshape(1, -1)

            # Normalize
            try:
                X_scaled = self.scaler.fit_transform(X)
            except Exception as e:
                logger.warning(f"Scaling error: {e}, using raw features")
                X_scaled = X

            # Predict: -1 = anomaly, 1 = normal
            prediction = self.model.predict(X_scaled)[0]

            # Get anomaly score (distance from decision boundary)
            # Negative score = anomaly, Positive score = normal
            decision_score = self.model.score_samples(X_scaled)[0]

            # Convert to 0-1 scale
            # Use sigmoid-like transformation: 1 / (1 + exp(score))
            anomaly_score = 1.0 / (1.0 + np.exp(decision_score))

            logger.debug(
                f"Anomaly detection complete: prediction={prediction}, "
                f"score={anomaly_score:.3f}"
            )

            return float(anomaly_score)

        except Exception as e:
            logger.error(f"Error in anomaly detection: {e}")
            return 0.0

    def _extract_features(self, metrics_data: Dict[str, Any]) -> list:
        """
        Extract feature vector from metrics

        Features extracted:
        1. Error rate deviation (current - baseline) / baseline
        2. Response time P95 deviation
        3. Response time P99 deviation
        4. Request rate change (%)
        5. Combined deviation score
        """
        features = []

        try:
            current = metrics_data.get("current", {})
            baseline = metrics_data.get("baseline", {})

            # Feature 1: Error rate deviation
            error_rate_current = current.get("error_rate", 0)
            error_rate_baseline = baseline.get("error_rate", 0.001)  # Avoid division by zero

            if error_rate_baseline > 0:
                error_deviation = (error_rate_current - error_rate_baseline) / error_rate_baseline
            else:
                error_deviation = error_rate_current * 100

            features.append(min(max(error_deviation, -10), 10))  # Clip to [-10, 10]

            # Feature 2: Response time P95 deviation
            resp_p95_current = current.get("response_time_p95", 0)
            resp_p95_baseline = baseline.get("response_time_p95", 0.001)

            if resp_p95_baseline > 0:
                resp_p95_deviation = (resp_p95_current - resp_p95_baseline) / resp_p95_baseline
            else:
                resp_p95_deviation = 0

            features.append(min(max(resp_p95_deviation, -10), 10))

            # Feature 3: Response time P99 deviation
            resp_p99_current = current.get("response_time_p99", 0)
            resp_p99_baseline = baseline.get("response_time_p99", 0.001)

            if resp_p99_baseline > 0:
                resp_p99_deviation = (resp_p99_current - resp_p99_baseline) / resp_p99_baseline
            else:
                resp_p99_deviation = 0

            features.append(min(max(resp_p99_deviation, -10), 10))

            # Feature 4: Request rate deviation
            request_rate_current = current.get("request_rate", 0)
            request_rate_baseline = baseline.get("request_rate", 0.001)

            if request_rate_baseline > 0:
                request_rate_deviation = (request_rate_current - request_rate_baseline) / request_rate_baseline
            else:
                request_rate_deviation = 0

            features.append(min(max(request_rate_deviation, -5), 5))

            # Feature 5: Combined deviation (weighted average)
            combined_score = (
                abs(error_deviation) * 0.4 +  # Error rate is most important
                abs(resp_p99_deviation) * 0.3 +  # P99 latency is critical
                abs(resp_p95_deviation) * 0.2 +
                abs(request_rate_deviation) * 0.1
            )

            features.append(combined_score)

            logger.debug(f"Extracted features: {[f'{f:.3f}' for f in features]}")

            return features

        except Exception as e:
            logger.error(f"Error extracting features: {e}")
            return None

    def score_to_severity(self, score: float) -> str:
        """Convert anomaly score to severity level"""
        if score > 0.85:
            return "CRITICAL"
        elif score > 0.70:
            return "HIGH"
        elif score > 0.50:
            return "MEDIUM"
        elif score > 0.30:
            return "LOW"
        else:
            return "INFO"
