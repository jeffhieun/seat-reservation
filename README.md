# Seat Reservation Platform

## 1. Overview

This project is a simplified seat reservation platform built as part of the LINKZ Lead Engineer Technical Assessment.

The system allows authenticated users to:

- Login
- View available seats
- Reserve a seat
- Complete a mock payment
- Confirm seat reservation after successful payment

### Key Engineering Considerations

This implementation focuses on:

- Security
- Transactional consistency
- Concurrency handling
- Payment reliability
- Operational simplicity

### Technology Stack

| Component | Technology |
|------------|------------|
| Backend | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Database Migration | Liquibase |
| Authentication | Spring Security |
| Cache | Caffeine |
| Build Tool | Gradle |
| Container Runtime | Podman |
| Testing | JUnit 5 |

---

## 2. High-Level Architecture

```text
React Client
      |
      v
+----------------+
| Spring Boot API|
+----------------+
      |
      v
+----------------+
| PostgreSQL     |
+----------------+
```

### Booking Flow

```text
User Login
    |
    v
View Available Seats
    |
    v
Reserve Seat
    |
    v
Seat -> PENDING_PAYMENT
    |
    v
Mock Payment
    |
    v
Payment Webhook
    |
    v
Reservation Confirmed
    |
    v
Seat -> RESERVED
```

### Concurrency Strategy

To prevent overselling:

- Reservation creation is transactional
- Seat records are locked using database row-level locking (`PESSIMISTIC_WRITE`)
- Only one user can reserve a seat at a time

### Payment Reliability

- Reservations remain in `PENDING_PAYMENT` until payment confirmation
- Duplicate webhooks are handled safely
- Expired reservations are automatically released

---

## 3. Project Structure

```text
seat-reservation/

├── README.md
├── build.gradle.kts
├── compose.yaml
├── Containerfile
│
├── src
│   ├── main
│   │
│   ├── java/com/linkz/reservation
│   │
│   │   ├── config
│   │   ├── common
│   │   ├── auth
│   │   ├── user
│   │   ├── seat
│   │   ├── reservation
│   │   ├── payment
│   │   └── scheduler
│   │
│   └── resources
│       ├── application.yml
│       └── db/changelog
│
└── test
    ├── ReservationServiceTest
    ├── PaymentServiceTest
    ├── ReservationConcurrencyTest
    └── BookingFlowIT
```

### Package Responsibilities

| Package | Responsibility |
|----------|---------------|
| auth | Authentication APIs |
| user | User management |
| seat | Seat inventory |
| reservation | Reservation workflow |
| payment | Payment processing |
| scheduler | Expiration cleanup jobs |
| common | Shared utilities and exception handling |
| config | Application configuration |

---

## 4. Local Setup Guide

### Prerequisites

Install:

- Java 21
- Gradle 9+
- Podman
- PostgreSQL (or Podman container)

Verify installation:

```bash
java -version
gradle -version
podman --version
```

### Start PostgreSQL

```bash
podman compose up -d postgres
```

### Configure Environment

Update `application.yml` if necessary:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/reservation
    username: postgres
    password: postgres
```

### Run Database Migrations

Liquibase runs automatically during application startup.

### Start Application

```bash
./gradlew bootRun
```

Application will start on:

```text
http://localhost:8080
```

### Run Tests

```bash
./gradlew test
```

### Health Check

```bash
GET http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

## Assumptions

- The system starts with exactly 3 available seats.
- Payment provider is mocked for assessment purposes.
- Local authentication is used for simplicity.
- OAuth/OIDC would be preferred in a production environment.
- PostgreSQL is used as the source of truth for consistency and concurrency control.

## Future Improvements

- OAuth/OIDC authentication
- Redis caching
- Kafka event-driven payment processing
- Real payment gateway integration
- Distributed tracing and metrics
- Horizontal scaling support