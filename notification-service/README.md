# Notification Service

Melih's Spring Boot service that owns the `Notification` aggregate
(Assignment 3 §3.7 REST API and §4.7 data model). It is the
user-facing inbox plus an internal send endpoint, designed to also
consume domain events from Order, Payment, and Delivery in later
checkpoints (A3 §6.2 / workflows W2 and W3).

Owner: Melih Arık.

## Layout

```
notification-service/
  pom.xml
  README.md
  mvnw, mvnw.cmd, .mvn/
  src/
    main/
      java/ee/ut/melih/notificationservice/
        NotificationServiceApplication.java   Spring Boot entry
        config/                               OpenApiConfig (Swagger metadata)
        controller/                           HTTP entry points + GlobalExceptionHandler
        service/                              business logic + MessageDispatcher abstraction
        repository/                           Spring Data JPA
        domain/                               Notification entity, Channel + Status enums
        dto/                                  request/response records + validation
        exception/                            NotificationNotFoundException, DispatchException
      resources/
        application.yaml                      default profile (PostgreSQL via env)
        application-dev.yaml                  H2 in-memory profile for local demo
    test/
      java/ee/ut/melih/notificationservice/
        NotificationServiceApplicationTests.java   context-loads smoke test
        controller/NotificationControllerTest.java @WebMvcTest, 3 cases
      resources/
        application-test.properties           H2 in-memory for tests
```

## Responsibilities

- Owns the `Notification` aggregate root: `id`, `recipientId`,
  `channel` (EMAIL | SMS | PUSH), `message`, `sentAt`, `status`
  (QUEUED | SENT | FAILED | READ). Schema is exactly A3 §4.7.
- Exposes the six A3 §3.7 endpoints — five user-facing inbox
  operations plus one internal `POST /notifications/send`.
- Persists to a service-local database (H2 in dev/test, PostgreSQL
  in prod). No other service reads this database directly.
- Treats `MessageDispatcher` as a pluggable boundary: today a
  log-only `LoggingMessageDispatcher`, swappable for a real
  email/SMS provider or a Kafka consumer in later checkpoints.
- Does **not** authenticate users itself. The API Gateway validates
  the bearer token and forwards the authenticated user id via the
  `X-User-Id` header. Token-based security on the service edge will
  be added in Checkpoint #3 (Project guidelines §4.3 D).
- Does **not** call other services synchronously. Inbound work
  reaches it as REST or, from Checkpoint #2 onward, as Kafka events
  (`payment-events`, `delivery-events`, `order-events`).

## API surface (A3 §3.7)

All routes are user-scoped via the `X-User-Id` request header.
`POST /notifications/send` is internal (used by other services or
by an admin/test action) and takes the recipient id in the body.

| Method | Path | Description |
|---|---|---|
| `GET` | `/notifications` | List notifications of the current user (paged via `page`, `size`) |
| `GET` | `/notifications/{id}` | Fetch one notification owned by the current user |
| `GET` | `/notifications/unread-count` | Unread count for the current user |
| `PATCH` | `/notifications/{id}/read` | Mark one notification as read |
| `PATCH` | `/notifications/read-all` | Mark every unread notification of the current user as read |
| `POST` | `/notifications/send` | Persist a new notification and dispatch it via `MessageDispatcher` |

Operational routes:

| Path | Description |
|---|---|
| `/swagger-ui.html` | Interactive API docs (Springdoc) |
| `/v3/api-docs` | OpenAPI 3 JSON |
| `/actuator/health` | Liveness probe |
| `/h2-console` | H2 web console (dev profile only) |

## Data model (A3 §4.7)

```
Notification (aggregate root)
  id              UUID, PK
  recipientId     UUID, cross-service reference to User Service
  channel         enum { EMAIL, SMS, PUSH }
  message         varchar(1024)
  sentAt          timestamp, set once dispatch succeeds
  status          enum { QUEUED, SENT, FAILED, READ }
```

Status transitions are owned by the entity (`markSent`, `markFailed`,
`markRead`); the service layer is the only caller.

## Workflows it participates in

- **Checkpoint #1 (now):** synchronous REST only — the six
  endpoints above plus the dispatcher boundary.
- **Checkpoint #2 (12 May):** the Event Broker shared component
  (Kafka, owned by Melih per A3 §3.9) is stood up; this service
  begins consuming real events.
- **Checkpoint #3 (19 May):** workflows **W2** (delivery progress)
  and **W3** (payment result) are wired end-to-end. Producers are
  Delivery and Payment; this service consumes from `delivery-events`
  and `payment-events` and creates user-facing notifications.

## Layered architecture

The service follows the layering required by Project guidelines §3.2.

```
HTTP request → NotificationController     (controller, no business logic)
             → SendNotificationRequest    (DTO, validated)
             → NotificationService        (business logic, transactions)
             → MessageDispatcher          (external boundary, mockable)
             → NotificationRepository     (Spring Data JPA)
             → Notification               (entity, owns its state)
             → database
```

DTOs are records (`SendNotificationRequest`, `NotificationResponse`,
`UnreadCountResponse`, `MarkAllReadResponse`, `ErrorResponse`); the
JPA entity is never serialised directly through the API.

## Run locally

### Quick start — H2 in-memory, no Docker required

```bash
cd notification-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Service comes up on `http://localhost:8087`. Open
`http://localhost:8087/swagger-ui.html` and exercise the endpoints
straight from the browser. Each user-scoped route asks for an
`X-User-Id` header — any UUID will do during the demo, e.g.
`11111111-1111-1111-1111-111111111111`.

### Production profile — PostgreSQL

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/notification_db \
SPRING_DATASOURCE_USERNAME=notification \
SPRING_DATASOURCE_PASSWORD=notification \
./mvnw spring-boot:run
```

Hibernate `ddl-auto: update` keeps the schema in sync at startup.
Each microservice owns its own database — no shared schema with
the other QuickBite services.

## Demo script (Swagger UI)

1. `POST /notifications/send` with body
   ```json
   {
     "recipientId": "11111111-1111-1111-1111-111111111111",
     "channel": "EMAIL",
     "message": "Order placed"
   }
   ```
   Expect `201 Created` and `status: SENT`.
2. `GET /notifications` with header
   `X-User-Id: 11111111-1111-1111-1111-111111111111` →
   the new notification appears in the list.
3. `GET /notifications/unread-count` → `{"unreadCount": 1}`.
4. `PATCH /notifications/read-all` → `{"updated": 1}`.
5. `GET /notifications/unread-count` → `{"unreadCount": 0}`,
   proving real persistence.

## Tests

```bash
cd notification-service
./mvnw test
```

Four tests across two classes:

- `NotificationServiceApplicationTests` — full-context smoke test
  on the `test` profile (H2).
- `NotificationControllerTest` — `@WebMvcTest(NotificationController)`
  with `NotificationService` imported and `MessageDispatcher` plus
  `NotificationRepository` mocked. Covers, per Project guidelines
  §4.1 E:
  - `send_happyPath_returns201_andMarksSent` — happy path with the
    mocked dispatcher succeeding; persisted status is `SENT`.
  - `send_dispatcherFails_returns502_andPersistsFailedStatus` —
    error path; the dispatcher throws and the service surfaces a
    `502 Bad Gateway` while persisting the failed attempt.
  - `send_invalidPayload_returns400` — DTO validation rejects an
    empty message.

`MessageDispatcher` is the cross-component dependency required by
the testing rubric: it abstracts the external messaging provider
(future email/SMS gateway or Kafka producer).

## Stack

- Java 17 (compiles and runs cleanly on Java 25)
- Spring Boot 3.3.4
- Spring Web, Spring Data JPA, Spring Validation, Spring Actuator
- Springdoc OpenAPI 2.5.0
- H2 (dev/test) and PostgreSQL (prod) via Hibernate
- JUnit 5, Mockito (Spring Boot Test starter)

## Related design documents

- A1 final submission — service ownership table.
- A3 §3.7 — REST API.
- A3 §4.7 — data model.
- A3 §6.2 — Kafka topics this service will consume from
  Checkpoint #2 onward.
- A3 §7 — final implementation responsibilities. Melih's second
  component is the Event Broker configuration (replaces Review
  Service, which stays design-only).
- Project guidelines §3.2 — required service layering.
- Project guidelines §4.1 — Checkpoint #1 deliverables this
  service is graded against.
