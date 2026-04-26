"""Prometheus client for fetching metrics"""

import logging
from typing import Dict, Any, Optional, List
from datetime import datetime, timedelta

import requests
from requests.exceptions import RequestException

logger = logging.getLogger(__name__)


class PrometheusClient:
    """Client for Prometheus queries"""

    def __init__(self, prometheus_url: str):
        self.base_url = prometheus_url.rstrip("/")
        self.timeout = 30

    def health_check(self) -> bool:
        """Check if Prometheus is reachable"""
        try:
            response = requests.get(
                f"{self.base_url}/-/healthy",
                timeout=self.timeout
            )
            return response.status_code == 200
        except RequestException as e:
            logger.error(f"Prometheus health check failed: {e}")
            raise

    def get_metrics_for_service(
        self,
        service_name: str,
        time_range: str = "30m",
        step: str = "1m"
    ) -> Optional[Dict[str, Any]]:
        """
        Fetch metrics for a service over time range

        Args:
            service_name: Name of the service (e.g., 'order-service')
            time_range: Time range (e.g., '30m', '1h')
            step: Query step interval

        Returns:
            Dictionary with metrics data
        """
        try:
            # Common metrics to query
            queries = {
                "error_rate": self._query_error_rate(service_name, time_range, step),
                "response_time_p95": self._query_response_time(service_name, "0.95", time_range, step),
                "response_time_p99": self._query_response_time(service_name, "0.99", time_range, step),
                "request_rate": self._query_request_rate(service_name, time_range, step),
                "cpu_usage": self._query_cpu_usage(service_name, time_range, step),
                "memory_usage": self._query_memory_usage(service_name, time_range, step),
            }

            results = {}
            for metric_name, query in queries.items():
                try:
                    results[metric_name] = self._execute_range_query(query, step)
                except Exception as e:
                    logger.warning(f"Failed to fetch {metric_name}: {e}")
                    results[metric_name] = None

            # Calculate baselines and deviations
            return self._calculate_statistics(results, service_name)

        except Exception as e:
            logger.error(f"Error fetching metrics for {service_name}: {e}")
            return None

    def _query_error_rate(self, service: str, time_range: str, step: str) -> str:
        """PromQL for error rate (5xx errors)"""
        return (
            f"rate(http_server_requests_seconds_count"
            f"{{service='{service}',status=~'5..'}}[1m])"
        )

    def _query_response_time(self, service: str, quantile: str, time_range: str, step: str) -> str:
        """PromQL for response time percentiles"""
        return (
            f"histogram_quantile({quantile},"
            f"rate(http_server_requests_seconds_bucket"
            f"{{service='{service}'}}[1m]))"
        )

    def _query_request_rate(self, service: str, time_range: str, step: str) -> str:
        """PromQL for request rate"""
        return (
            f"rate(http_server_requests_seconds_count"
            f"{{service='{service}'}}[1m])"
        )

    def _query_cpu_usage(self, service: str, time_range: str, step: str) -> str:
        """PromQL for CPU usage (if available)"""
        return (
            f"container_cpu_usage_seconds_total"
            f"{{pod=~'{service}.*'}}"
        )

    def _query_memory_usage(self, service: str, time_range: str, step: str) -> str:
        """PromQL for memory usage (if available)"""
        return (
            f"container_memory_usage_bytes"
            f"{{pod=~'{service}.*'}}"
        )

    def _execute_range_query(self, query: str, step: str) -> Optional[List[tuple]]:
        """Execute a Prometheus range query"""
        try:
            params = {
                "query": query,
                "start": (datetime.now() - timedelta(hours=1)).isoformat(),
                "end": datetime.now().isoformat(),
                "step": step
            }

            response = requests.get(
                f"{self.base_url}/api/v1/query_range",
                params=params,
                timeout=self.timeout
            )

            if response.status_code != 200:
                logger.warning(f"Prometheus query failed: {response.text}")
                return None

            data = response.json()
            if data["status"] != "success":
                logger.warning(f"Query not successful: {data}")
                return None

            # Extract values
            results = []
            for series in data.get("data", {}).get("result", []):
                for timestamp, value in series.get("values", []):
                    try:
                        results.append((
                            datetime.fromtimestamp(int(timestamp)),
                            float(value)
                        ))
                    except (ValueError, TypeError):
                        continue

            return sorted(results) if results else None

        except RequestException as e:
            logger.error(f"HTTP error during Prometheus query: {e}")
            return None

    def _calculate_statistics(self, metrics: Dict[str, Any], service: str) -> Dict[str, Any]:
        """Calculate baseline, current value, and deviation for metrics"""
        stats = {
            "service": service,
            "timestamp": datetime.now().isoformat(),
            "current": {},
            "baseline": {},
            "deviation_percent": {}
        }

        for metric_name, values in metrics.items():
            if not values or len(values) < 2:
                stats["current"][metric_name] = None
                stats["baseline"][metric_name] = None
                stats["deviation_percent"][metric_name] = None
                continue

            values_only = [v for _, v in values]

            # Current value (last 5-minute average)
            current = sum(values_only[-5:]) / len(values_only[-5:]) if len(values_only) >= 5 else values_only[-1]

            # Baseline (first 10-minute average)
            baseline = sum(values_only[:10]) / len(values_only[:10]) if len(values_only) >= 10 else sum(values_only) / len(values_only)

            # Deviation percentage
            if baseline != 0:
                deviation_percent = ((current - baseline) / baseline) * 100
            else:
                deviation_percent = 0 if current == 0 else 100

            stats["current"][metric_name] = round(current, 6)
            stats["baseline"][metric_name] = round(baseline, 6)
            stats["deviation_percent"][metric_name] = round(deviation_percent, 2)

        return stats
