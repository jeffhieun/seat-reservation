# Seat Reservation Platform

Secure seat reservation backend with JWT authentication, concurrency protection, and payment webhook handling.

## Prerequisites

- Java 21
- Gradle
- Podman

## Build

```bash
./gradlew clean build
podman build -t seat-reservation-api:latest .
```

## Run

```bash
podman compose up -d
```

## Verify

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Stop

```bash
podman compose down
```

## API Endpoints

Main Swagger UI: http://localhost:8080/swagger-ui.html
Redirects to: http://localhost:8080/swagger-ui/index.html
OpenAPI JSON Documentation: http://localhost:8080/v3/api-docs
Health Check: http://localhost:8080/actuator/health

## Demo User

Email: test@example.com  
Password: password123

## Technology Stack

- Java 21, Spring Boot 3.5
- PostgreSQL 17
- JWT Authentication
- Liquibase Migrations
- Podman
