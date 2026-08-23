# Micro Observe Kafka

An event-driven Spring Boot microservices project with MySQL, Kafka, Keycloak,
Prometheus, Grafana, Loki, and Tempo.

## Run locally

1. Copy `.env.example` to `.env` and set all four passwords.
2. Start the infrastructure with `docker compose up -d`.
3. Export the same `MYSQL_ROOT_PASSWORD` before starting the Spring Boot services.
4. Run `sh ./mvnw verify` to build and test every service.

The product, inventory, and order services each own a separate MySQL database.
Flyway creates and validates their schemas on application startup.
