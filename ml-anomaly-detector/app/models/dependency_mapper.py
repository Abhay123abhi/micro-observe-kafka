"""Service Dependency Mapper"""

import logging
from typing import Dict, List, Set

logger = logging.getLogger(__name__)


class DependencyMapper:
    """Maps service dependencies and detects cascading failures"""

    # Define service dependency graph
    DEPENDENCY_GRAPH = {
        "api-gateway": ["order-service", "product-service", "inventory-service"],
        "order-service": ["inventory-service", "notification-service"],
        "inventory-service": [],
        "product-service": [],
        "notification-service": []
    }

    def __init__(self):
        logger.info("✓ DependencyMapper initialized")

    def get_impacted_services(self, service_name: str) -> List[str]:
        """
        Get services impacted if this service fails

        Args:
            service_name: Name of failed service

        Returns:
            List of impacted services
        """
        try:
            impacted = []

            # Find services that depend on this one
            for service, dependencies in self.DEPENDENCY_GRAPH.items():
                if service_name in dependencies:
                    impacted.append(service)

            logger.debug(f"{service_name} impact: {impacted}")
            return impacted

        except Exception as e:
            logger.error(f"Error getting impacted services: {e}")
            return []

    def get_service_dependencies(self, service_name: str) -> List[str]:
        """Get dependencies of a service"""
        return self.DEPENDENCY_GRAPH.get(service_name, [])

    def check_cascading_failure(self, failed_services: List[str]) -> Dict[str, List[str]]:
        """
        Analyze cascading failures

        Args:
            failed_services: List of failed services

        Returns:
            Dictionary with affected services at each level
        """
        affected = {}
        visited = set(failed_services)

        for service in failed_services:
            affected[service] = self.get_impacted_services(service)
            visited.update(affected[service])

        return affected

    def get_critical_path(self) -> List[str]:
        """Get critical services (many dependents)"""
        dependent_count = {}

        for service, dependencies in self.DEPENDENCY_GRAPH.items():
            for dep in dependencies:
                dependent_count[dep] = dependent_count.get(dep, 0) + 1

        # Sort by number of dependents
        critical = sorted(
            dependent_count.items(),
            key=lambda x: x[1],
            reverse=True
        )

        return [svc for svc, _ in critical]
