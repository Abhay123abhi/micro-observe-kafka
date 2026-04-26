# Add this service to api-gateway/docker-compose.yml

  ml-anomaly-detector:
    build:
      context: ../ml-anomaly-detector
      dockerfile: docker/Dockerfile
    container_name: ml-anomaly-detector
    ports:
      - '8000:8000'
    environment:
      PROMETHEUS_URL: http://prometheus:9090
      KAFKA_BOOTSTRAP_SERVERS: broker:29092
      KAFKA_OUTPUT_TOPIC: alerts-ml
      LOKI_URL: http://loki:3100
      ANOMALY_THRESHOLD: '0.6'
      SAMPLING_INTERVAL: '30'
      LOG_LEVEL: 'INFO'
    depends_on:
      - prometheus
      - broker
      - loki
    networks:
      - default
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    profiles:
      - ml  # Run with: docker-compose --profile ml up
