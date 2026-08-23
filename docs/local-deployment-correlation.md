# Local deployment correlation walkthrough

A Docker Compose recreate is a deployment in this project. Record the event only after the new container is healthy.

## 1. Deploy a service locally

~~~powershell
docker compose up -d --build --force-recreate inventory-service
curl.exe -i http://localhost:8082/actuator/health
~~~

Wait for HTTP 200.

## 2. Record the completed deployment

~~~powershell
$body = @{
  service = "inventory-service"
  environment = "local"
  version = "local-1.1.0"
  gitCommit = (git rev-parse --short HEAD)
  changeSummary = "Local deployment correlation test"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:9000/api/deployments" `
  -ContentType "application/json" `
  -Body $body
~~~

The event is stored in PostgreSQL and published through the transactional outbox to the `deployment-events` Kafka topic.

## 3. Trigger a controlled incident

With the `demo` profile enabled:

~~~powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8082/demo/failures?latencyMillis=5000&failRequests=false"
~~~

Generate traffic through the gateway until the Prometheus latency rule fires. The investigation includes the five most recent deployments for the affected service from the preceding 30 minutes.

## 4. Verify the correlation

~~~powershell
Invoke-RestMethod "http://localhost:9000/api/incidents?scope=all&page=0&size=20" |
  ConvertTo-Json -Depth 8

docker compose logs --tail=200 ai-incident-analyzer
~~~

The report should refer to the deployment as a correlated change. It is not proof of causation and must not trigger an automatic rollback.

Remove the controlled failure when finished:

~~~powershell
Invoke-RestMethod -Method Delete -Uri "http://localhost:8082/demo/failures"
~~~
