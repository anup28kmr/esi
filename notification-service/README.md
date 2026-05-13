# Notification Service

Melih's Spring Boot service that owns the `Notification` aggregate
(Assignment 3 §3.7 REST API and §4.7 data model). It is the
user-facing inbox plus the consumer for `payment-events`,
`delivery-events`, and `order-events` (A3 §6.2 / workflows W2, W3).

Owner: **Melih Arık** — service + Event Broker (Kafka) configuration.

---

## How other services trigger a notification

**Publish a Kafka event.** Do **not** call this service over REST.

The Notification Service listens on three topics with the
group id `notification-service`:

| Topic | Producer | Example types |
|---|---|---|
| `payment-events` | Payment Service (Ege) | `PAYMENT_CONFIRMED`, `PAYMENT_FAILED`, `PAYMENT_REFUNDED` |
| `delivery-events` | Delivery Service (Ege) | `DELIVERY_ASSIGNED`, `DELIVERY_DISPATCHED`, `DELIVERY_COMPLETED` |
| `order-events` | Order Service (Anup) | `ORDER_PLACED`, `ORDER_CONFIRMED`, `ORDER_CANCELLED` |

### Event envelope (`EventEnvelope`)

```json
{
  "id": "65adfb1e-0d44-4f3e-9b6b-4d8a3b7f2c11",
  "type": "PAYMENT_CONFIRMED",
  "occurredAt": "2026-05-13T15:42:11Z",
  "payload": {
    "orderId": 4231,
    "recipientId": "11111111-1111-1111-1111-111111111111",
    "message": "Your payment of $24.50 was confirmed"
  }
}
```

Field rules:

- `id` — UUID, idempotency key for the event. Use `UUID.randomUUID()`.
- `type` — short upper-snake-case event name. Reaches the frontend as
  the icon + title (`PAYMENT_CONFIRMED` → 💳 "Payment Confirmed").
- `occurredAt` — ISO-8601 instant in UTC. `Instant.now()` is fine.
- `payload.recipientId` — **required UUID** of the user to notify.
  The consumer also accepts `customerId` or `userId` as fallbacks.
  Events without any of these are skipped with a warning.
- `payload.message` — optional human-readable text. If omitted, the
  consumer falls back to a generic message derived from the event
  `type`.

Any extra fields you put in `payload` are tolerated (we use Jackson
`spring.json.trusted.packages: "*"`), so feel free to include
`orderId`, `restaurantId`, etc. for traceability.

### Spring producer example

```java
// In your service (Payment, Delivery, Order, ...):
@Autowired KafkaTemplate<String, Object> kafkaTemplate;

void notifyPaymentConfirmed(UUID customerId, long orderId, BigDecimal amount) {
  Map<String, Object> payload = Map.of(
      "orderId",     orderId,
      "recipientId", customerId.toString(),
      "message",     "Your payment of $" + amount + " was confirmed"
  );
  Map<String, Object> event = Map.of(
      "id",         UUID.randomUUID().toString(),
      "type",       "PAYMENT_CONFIRMED",
      "occurredAt", Instant.now().toString(),
      "payload",    payload
  );
  kafkaTemplate.send("payment-events", customerId.toString(), event);
}
```

Required dependency in your `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```

Required `application.yaml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

The `kafka` service hostname inside the compose network is `kafka`
(see `docker-compose.yml`). On the host it's `localhost:9092`.

### What the consumer does

1. Receives the event (`KafkaEventConsumer`).
2. Resolves `recipientId` from the payload.
3. Creates a `Notification` with channel `PUSH`, the message text,
   and `eventType = event.type()`.
4. Persists + sets status to `SENT`.
5. The user sees it instantly on the frontend (5-second polling).

### Quick test from any machine

Without a producer service handy, you can still publish an event by
talking to Redpanda directly:

```bash
docker exec -i quickbite-kafka-1 rpk topic produce payment-events --key=order-42 <<'EOF'
{"id":"$(uuidgen)","type":"PAYMENT_CONFIRMED","occurredAt":"2026-05-13T15:00:00Z","payload":{"orderId":42,"recipientId":"11111111-1111-1111-1111-111111111111","message":"Test notification"}}
EOF
```

---

## REST API

All routes are protected by JWT bearer-token auth (`Authorization:
Bearer <token>`) issued by User Service. The token's `userId` claim
is the source of truth — there is **no** client-supplied user header.

Reach the service through the API Gateway:

```
http://localhost:9090/api/notifications/...
```

(Direct hits to `http://localhost:8087/...` also work for debugging,
provided you carry the bearer token.)

| Method | Path | Description | Token type |
|---|---|---|---|
| `GET` | `/api/notifications` | List the current user's notifications (paged via `page`, `size`) | User |
| `GET` | `/api/notifications/{id}` | Fetch one notification owned by the current user | User |
| `GET` | `/api/notifications/unread-count` | Unread count for the current user | User |
| `PATCH` | `/api/notifications/{id}/read` | Mark one notification as read | User |
| `PATCH` | `/api/notifications/read-all` | Mark every unread notification as read | User |
| `POST` | `/api/notifications/send` | Push a notification on behalf of a user (Kafka is preferred) | **SERVICE** only |

**Why prefer Kafka over `POST /send`?** Async decouples your service
from notification — if Notification is down, your event sits on
Kafka and gets consumed when it recovers. Synchronous `POST /send`
fails your request when Notification is unavailable. Use `/send`
only when you genuinely need confirmation that the user was
notified before returning from your call.

### Sample response

```json
{
  "id": "acc4fb82-7cbe-4d90-9b47-fe3c262e6324",
  "recipientId": "485bd142-a3f4-4654-a88a-090e17599b02",
  "channel": "PUSH",
  "message": "Your order is on its way",
  "eventType": "DELIVERY_DISPATCHED",
  "sentAt": "2026-05-13T17:59:17.415Z",
  "status": "SENT"
}
```

### Operational routes

| Path | Description |
|---|---|
| `/swagger-ui.html` | Interactive API docs (Springdoc) |
| `/v3/api-docs` | OpenAPI 3 JSON |
| `/actuator/health` | Liveness probe (used by Compose) |

---

## Data model (A3 §4.7)

```
Notification (aggregate root)
  id              UUID, PK
  recipientId     UUID, cross-service reference to User Service
  channel         enum { EMAIL, SMS, PUSH }
  message         varchar(1024)
  eventType       varchar(64), nullable — original Kafka event type
  sentAt          timestamp, set once dispatch succeeds
  status          enum { QUEUED, SENT, FAILED, READ }
```

Status transitions are owned by the entity (`markSent`, `markFailed`,
`markRead`); the service layer is the only caller.

---

## Security

- `JwtAuthFilter` validates the bearer token locally using the
  shared HMAC secret (`JWT_SECRET`) and issuer (`JWT_ISSUER` =
  `quickbite-user-service`). Both are set in `docker-compose.yml`
  via the `&jwt-env` anchor; every Spring service uses the same
  values so a token issued by User Service is accepted everywhere.
- `SecurityConfig` requires authentication on all routes except
  Swagger / actuator-health / CORS preflight.
- The `userId` UUID is read from the token's `userId` claim and
  injected into the controller via `@AuthenticationPrincipal
  AuthenticatedUser`. **A spoofed `X-User-Id` header has no effect.**
- `POST /notifications/send` checks `AuthenticatedUser.isService()`
  (i.e. `tokenType == "SERVICE"`) and returns 403 otherwise.

---

## Run locally

### As part of the full stack (recommended)

```bash
cd ..        # repo root
docker-compose up -d notification-service notification-db kafka
```

The notification stack starts in ~30 s. Add `api-gateway`, `user-service`,
and `frontend` to log in and test from the browser.

### Standalone with PostgreSQL

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5436/notification_db \
SPRING_DATASOURCE_USERNAME=notification \
SPRING_DATASOURCE_PASSWORD=notification \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
JWT_SECRET=dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY= \
JWT_ISSUER=quickbite-user-service \
./mvnw spring-boot:run
```

Hibernate `ddl-auto: update` keeps the schema in sync at startup.

---

## Tests

```bash
./mvnw test
```

`NotificationControllerTest` (`@WebMvcTest`) covers:

- `send_happyPath_returns201_andMarksSent` — SERVICE token, dispatcher
  mocked to succeed.
- `send_dispatcherFails_returns502_andPersistsFailedStatus` — SERVICE
  token, dispatcher throws.
- `send_invalidPayload_returns400` — DTO validation.
- `send_userTokenWithoutServiceRole_returns403` — USER token rejected.
- `send_anonymous_returns401` — no token rejected.

`MessageDispatcher` is the cross-component dependency required by
the testing rubric (Project guidelines §4.1 E).

---

## Stack

- Java 21
- Spring Boot 3.3.4 — Web, Data JPA, Validation, Actuator, Security
- Spring Kafka (Apache Kafka protocol; we run Redpanda in the
  compose network — drop-in compatible)
- Springdoc OpenAPI 2.5.0
- PostgreSQL 15 (prod), H2 (tests)
- jjwt 0.11.5
- JUnit 5, Mockito, Spring Security Test

---

## Related design documents

- A3 §3.7 — REST API.
- A3 §3.9 — Event Broker (shared integration component, also Melih).
- A3 §4.7 — data model.
- A3 §6.2 — Kafka topics this service consumes from.
- A3 §7 — final implementation responsibilities.
- Project guidelines §3.2 — required service layering.
- Project guidelines §4.3 — Checkpoint #3 deliverables.
