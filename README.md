Updated `README.md` removing **Development Notes** section:

````md
# Seat Reservation Platform

## Overview

A full-stack seat reservation platform.

The project includes:

- Backend API: Java 21 + Spring Boot
- Frontend application: Node.js 20+
- Database: PostgreSQL
- Container runtime: Podman

The application supports:

- User authentication
- Seat reservation management
- REST API
- Swagger API documentation
- Containerized deployment

---

# Quick Start

From the project root directory:

```bash
chmod +x run-all.sh
./run-all.sh
````

The startup script will automatically:

1. Check Podman machine status
2. Start Podman machine if it is not running
3. Remove existing containers
4. Build backend container
5. Start backend service
6. Build frontend container
7. Start frontend service

After startup, the application is ready.

---

# Application URLs

| Service              | URL                                                                                        |
| -------------------- | ------------------------------------------------------------------------------------------ |
| Frontend Application | [http://localhost:3000](http://localhost:3000)                                             |
| Backend API          | [http://localhost:8080](http://localhost:8080)                                             |
| Swagger UI           | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Health Check         | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)             |

---

# Authentication Test Accounts

The application provides pre-configured test accounts.

Use these accounts to test the login system.

| User          | Email                                       | Password    |
| ------------- | ------------------------------------------- | ----------- |
| Test User 001 | [test001@test.com](mailto:test001@test.com) | password123 |
| Test User 002 | [test002@test.com](mailto:test002@test.com) | password123 |
| Test User 003 | [test003@test.com](mailto:test003@test.com) | password123 |
| Test User 004 | [test004@test.com](mailto:test004@test.com) | password123 |

Example login:

```text
Email:
test001@test.com

Password:
password123
```

---

# API Testing

## Swagger

Open:

```text
http://localhost:8080/swagger-ui/index.html
```

You can test:

* Authentication API
* User API
* Seat reservation API

---

# Container Logs

## Backend Logs

```bash
podman logs seat-reservation-backend
```

## Frontend Logs

```bash
podman logs seat-reservation-frontend
```

---

# Project Structure

```text
seat-reservation/

├── run-all.sh

├── backend/
│   ├── run-backend.sh
│   └── src/

├── frontend/
│   ├── run-frontend.sh
│   └── src/
```

---

# Troubleshooting

Restart everything:

```bash
./run-all.sh
```

Check application health:

```bash
curl http://localhost:8080/actuator/health
```

```
```
