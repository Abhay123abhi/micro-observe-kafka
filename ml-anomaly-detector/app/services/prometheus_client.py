"""Prometheus client service - moved to separate file"""

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
            service_name: Name of the service
            time_range: Time range for query
            step: Query step interval

        Returns:
            Dictionary with metrics data
        """
        try:
            # Common metrics to query
            queries = {
                "error_rate": self._query_error_rate(service_name),
                "response_time_p95": self._query_response_time(service_name, "0.95"),
                "response_time_p99": self._query_response_time(service_name, "0.99"),
                "request_rate": self._query_request_rate(service_name),
            }

            results = {}
            for metric_name, query in queries.items():
                try:
                    results[metric_name] = self._execute_range_query(query, time_range, step)
                except Exception as e:
                    logger.warning(f"Failed to fetch {metric_name}: {e}")
                    results[metric_name] = None

            return self._calculate_statistics(results, service_name)

        except Exception as e:
            logger.error(f"Error fetching metrics for {service_name}: {e}")
            return None

    def _query_error_rate(self, service: str) -> str:
        """PromQL for error rate"""
        return (
            f"rate(http_server_requests_seconds_count"
            f"{{application='{service}',status=~'5..'}}[1m])"
        )

    def _query_response_time(self, service: str, quantile: str) -> str:
        """PromQL for response time"""
        return (
            f"histogram_quantile({quantile},"
            f"rate(http_server_requests_seconds_bucket"
            f"{{application='{service}'}}[1m]))"
        )

    def _query_request_rate(self, service: str) -> str:
        """PromQL for request rate"""
        return (
            f"rate(http_server_requests_seconds_count"
            f"{{application='{service}'}}[1m])"
        )

    def _execute_range_query(self, query: str, time_range: str, step: str) -> Optional[List[tuple]]:
        """Execute Prometheus range query"""
        try:
            # Calculate time range
            end_time = datetime.utcnow()
            if time_range.endswith("m"):
                minutes = int(time_range[:-1])
                start_time = end_time - timedelta(minutes=minutes)
            elif time_range.endswith("h"):
                hours = int(time_range[:-1])
                start_time = end_time - timedelta(hours=hours)
            else:
                start_time = end_time - timedelta(minutes=30)

            params = {
                "query": query,
                "start": int(start_time.timestamp()),
                "end": int(end_time.timestamp()),
                "step": step
            }

            response = requests.get(
                f"{self.base_url}/api/v1/query_range",
                params=params,
                timeout=self.timeout
            )

            if response.status_code != 200:
                logger.warning(f"Query failed: {response.text}")
                return None

            data = response.json()
            if data["status"] != "success":
                return None

            results = []
            for series in data.get("data", {}).get("result", []):
                for timestamp, value in series.get("values", []):
                    try:
                        results.append((int(timestamp), float(value)))
                    except (ValueError, TypeError):
                        continue

            return sorted(results) if results else None

        except RequestException as e:
            logger.error(f"Prometheus query error: {e}")
            return None

    def _calculate_statistics(self, metrics: Dict[str, Any], service: str) -> Dict[str, Any]:
        """Calculate statistics for metrics"""
        stats = {
            "service": service,
            "current": {},
            "baseline": {}
        }

        for metric_name, values in metrics.items():
            if not values or len(values) < 2:
                stats["current"][metric_name] = 0
                stats["baseline"][metric_name] = 0
                continue

            values_only = [v for _, v in values]
            current = sum(values_only[-5:]) / min(5, len(values_only))
            baseline = sum(values_only[:10]) / min(10, len(values_only))

            stats["current"][metric_name] = round(current, 6)
            stats["baseline"][metric_name] = round(baseline, 6)

        return stats
