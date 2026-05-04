# Notification Service

QuickBite Notification Service — Owner: Melih Arık.
Implements §3.7 (REST API) and §4.7 (data model) of Assignment 3.

## Run locally

### Quick start (in-memory H2 — no Docker needed)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Service starts on `http://localhost:8087`.

- Swagger UI: http://localhost:8087/swagger-ui.html
- OpenAPI JSON: http://localhost:8087/v3/api-docs
- Health: http://localhost:8087/actuator/health
- H2 console: http://localhost:8087/h2-console (dev profile only)

### Production profile (PostgreSQL)

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/notification_db \
SPRING_DATASOURCE_USERNAME=notification \
SPRING_DATASOURCE_PASSWORD=notification \
./mvnw spring-boot:run
```

## Endpoints (per A3 §3.7)

All endpoints expect the authenticated user id in the `X-User-Id` header (the API
Gateway propagates it after token validation). `POST /notifications/send` is internal.

| Method | Path | Description |
|---|---|---|
| GET | `/notifications` | List notifications of the current user |
| GET | `/notifications/{id}` | Get a single notification |
| GET | `/notifications/unread-count` | Number of unread notifications |
| PATCH | `/notifications/{id}/read` | Mark a notification as read |
| PATCH | `/notifications/read-all` | Mark all of the current user's notifications as read |
| POST | `/notifications/send` | Internal send (admin/test) |

## Run tests

```bash
./mvnw test
```
