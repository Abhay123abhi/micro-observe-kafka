"""Kafka producer for sending alerts"""

import json
import logging
from typing import Dict, Any, Optional, List

from kafka import KafkaProducer as NativeKafkaProducer

logger = logging.getLogger(__name__)


class KafkaProducer:
    """Producer for sending messages to Kafka"""

    def __init__(self, bootstrap_servers: List[str], topic: str = "alerts-ml"):
        """
        Initialize Kafka producer

        Args:
            bootstrap_servers: List of Kafka broker addresses
            topic: Default topic for alerts
        """
        self.bootstrap_servers = bootstrap_servers
        self.default_topic = topic

        try:
            self.producer = NativeKafkaProducer(
                bootstrap_servers=bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8") if k else None,
                acks="all",
                retries=3,
                max_in_flight_requests_per_connection=5
            )
            logger.info(f"✓ Kafka producer initialized with brokers: {bootstrap_servers}")
        except Exception as e:
            logger.error(f"Failed to initialize Kafka producer: {e}")
            raise

    def send(
        self,
        message: Dict[str, Any],
        topic: Optional[str] = None,
        key: Optional[str] = None,
        callback: Optional[callable] = None
    ) -> bool:
        """
        Send message to Kafka topic

        Args:
            message: Message payload
            topic: Topic name (uses default if not specified)
            key: Message key for partitioning
            callback: Optional callback function

        Returns:
            True if send succeeded, False otherwise
        """
        topic = topic or self.default_topic

        try:
            future = self.producer.send(
                topic,
                value=message,
                key=key
            )

            # Wait for send to complete
            record_metadata = future.get(timeout=10)

            logger.info(
                f"✓ Alert sent to Kafka topic '{topic}' "
                f"[partition={record_metadata.partition}, offset={record_metadata.offset}]"
            )

            if callback:
                callback(record_metadata)

            return True

        except Exception as e:
            logger.error(f"Failed to send message to Kafka: {e}")
            return False

    def send_batch(self, messages: List[Dict[str, Any]], topic: Optional[str] = None) -> int:
        """
        Send multiple messages to Kafka

        Args:
            messages: List of messages
            topic: Topic name

        Returns:
            Number of successfully sent messages
        """
        topic = topic or self.default_topic
        sent_count = 0

        for message in messages:
            if self.send(message, topic=topic):
                sent_count += 1

        self.producer.flush()
        logger.info(f"Sent {sent_count}/{len(messages)} messages to Kafka")

        return sent_count

    def close(self) -> None:
        """Close Kafka producer connection"""
        try:
            self.producer.flush()
            self.producer.close()
            logger.info("✓ Kafka producer closed")
        except Exception as e:
            logger.error(f"Error closing Kafka producer: {e}")
