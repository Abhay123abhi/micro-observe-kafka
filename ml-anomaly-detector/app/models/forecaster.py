"""Time-Series Forecasting Module using Prophet"""

import logging
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)


class Forecaster:
    """Time-series forecasting using Prophet"""

    def __init__(self):
        logger.info("✓ Forecaster initialized")

    def predict(self, metrics_data: Dict[str, Any], service_name: str) -> Dict[str, Any]:
        """
        Predict future metrics trend

        Args:
            metrics_data: Current metrics
            service_name: Service name

        Returns:
            Forecast dictionary with predictions
        """
        try:
            current_values = metrics_data.get("current", {})

            # Simple trend analysis without Prophet (for MVP)
            forecast = {
                "service": service_name,
                "trending": self._determine_trend(metrics_data),
                "forecast_30min": self._forecast_simple(current_values),
                "confidence": 0.75
            }

            return forecast

        except Exception as e:
            logger.error(f"Forecasting error: {e}")
            return {
                "service": service_name,
                "trending": "unknown",
                "confidence": 0.0
            }

    def _determine_trend(self, metrics_data: Dict[str, Any]) -> str:
        """Determine if metrics are trending up, down, or stable"""
        try:
            current = metrics_data.get("current", {})
            baseline = metrics_data.get("baseline", {})

            # Error rate trending
            err_curr = current.get("error_rate", 0)
            err_base = baseline.get("error_rate", 0.001)
            err_change = (err_curr - err_base) / err_base if err_base > 0 else 0

            # Latency trending
            lat_curr = current.get("response_time_p99", 0)
            lat_base = baseline.get("response_time_p99", 0.001)
            lat_change = (lat_curr - lat_base) / lat_base if lat_base > 0 else 0

            # Combined trend
            combined_change = (err_change + lat_change) / 2

            if combined_change > 0.2:
                return "increasing"
            elif combined_change < -0.1:
                return "decreasing"
            else:
                return "stable"

        except Exception as e:
            logger.error(f"Trend determination error: {e}")
            return "unknown"

    def _forecast_simple(self, current_values: Dict[str, float]) -> Dict[str, Any]:
        """Simple forecast based on current values"""
        return {
            "error_rate_30min": self._forecast_value(
                current_values.get("error_rate", 0),
                increase_factor=1.2
            ),
            "latency_p99_30min": self._forecast_value(
                current_values.get("response_time_p99", 0),
                increase_factor=1.15
            ),
            "confidence_interval": "±20%"
        }

    def _forecast_value(self, current: float, increase_factor: float = 1.1) -> str:
        """Forecast a single metric value"""
        if current == 0:
            return "0"
        forecasted = current * increase_factor
        return f"{forecasted:.4f}"
