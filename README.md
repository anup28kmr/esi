# Enterprise System Integration in a Food-Delivery App

Project spec: https://courses.cs.ut.ee/2026/esi/spring/Main/Lectures?action=download&upname=Project2026.pdf

For grading of project work by Group 7 at the Checkpoint 2 stage.

## Grading rubrics for Checkpoint 2

**Total: 8 points.** Deadline: last commit at **12 May 2026, 14:00 (Estonian time)**; team discussions start the same day at 14:15.

**Goal:** Complete backend responsibilities and start system integration. Security is not assessed at this checkpoint (it is graded at Checkpoint 3).

**Per-student requirement:** Each student must show their **second service** implemented, OR their **integration/resilience component** implemented and usable. Tests and documentation are **not** required for this second responsibility — only points A, B, and D from Checkpoint 1 apply (Running Service, API Implementation, Persistence).

### Deliverables (per student)

- [ ] **A. Second Responsibility — 4 pts:** second service OR integration/resilience component runs
    - [ ] Running Service: service starts and endpoints are accessible
    - [ ] API Implementation: all endpoints from Assignment 3 implemented
    - [ ] Persistence: database connected; data stored and retrieved (services only)
    - [ ] Layered structure respected: Controller → DTO → Service → Repository → Domain
    - [ ] ~5–8 REST endpoints exposed (services only)
- [ ] **B. Basic Integration — 2 pts:** at least one working interaction between two implemented services demonstrated via a **real call** (not mocked)
- [ ] **C. Initial Frontend — 2 pts:** frontend exists, calls at least one backend endpoint per student, and displays real data

### Grading summary

| # | Criterion                       | Points |
|---|---------------------------------|--------|
| A | Second service/component runs   | 4      |
| B | Basic integration (real call)   | 2      |
| C | Initial frontend                | 2      |
|   | **Total**                       | **8**  |

### Demonstration

- [ ] API demo of already implemented endpoints via Postman or the frontend (no Swagger required for the second responsibility)

## To course instructors: How to quickly verify that code repository meets grading rubrics for Checkpoint 2

**Prerequisites:** Docker Desktop running. The Item B verification uses `scripts/mint-jwt.sh`, which needs only `bash` + `openssl` (both ship with Git Bash on Windows; native on Linux/Mac). Windows users: run the shell snippets below in Git Bash or WSL.

### 1. Bring the stack up

From the repository root:

```bash
docker compose up -d --build
```

First-time build is ~3–5 minutes (Maven dependencies + `npm ci` + Vue CLI build). Subsequent runs come up in well under a minute. Confirm all six containers reach `healthy`:

```bash
docker compose ps
```

Expected services: `restaurant-db`, `menu-db`, `restaurant-service`, `menu-service`, `gateway`, `frontend`.

**If host ports are taken.** The two Postgres containers default to `5432` (restaurant) and `5433` (menu) on the host. If a local Postgres or another stack already binds those, set `RESTAURANT_DB_HOST_PORT` and/or `MENU_DB_HOST_PORT` before bringing the stack up. Application code is unaffected because the in-network port stays `5432` for both databases.

```bash
RESTAURANT_DB_HOST_PORT=15432 MENU_DB_HOST_PORT=15433 docker compose up -d --build
```

**Gateway path conventions.** `gateway/nginx.conf` exposes each Sierra-Lima endpoint twice. The bare form (`/restaurants`, `/menu-items`) is for `curl` and server-to-server callers; the `/api/`-prefixed form (`/api/restaurants`, `/api/menu-items`) is for the browser SPA, whose `client.js` targets `/api/**`. The README curls below use the bare form so the output stays the same when the team's real Spring Cloud Gateway lands.

### 2. Item A — second service runs with real Postgres persistence (4 pts)

Open the Swagger UIs in a browser:

- http://localhost:8081/swagger-ui.html — `restaurant-service` (R19 register/manage restaurant, R20 status toggle)
- http://localhost:8082/swagger-ui.html — `menu-service` (R21 owner-gated CRUD, R22 browse menu)

Or via curl through the gateway. Both responses are served by Spring Boot from the live Postgres databases — Flyway migrations `V1__init.sql` and `V2__seed_demo_data.sql` are applied at container start:

```bash
# 6 restaurants seeded in restaurant-db:
curl -s "http://localhost:8080/restaurants?size=2" | head -c 400 ; echo

# 4 seeded menu items for restaurant d0000001 (Pizza Antonio) in menu-db:
curl -s "http://localhost:8080/restaurants/d0000001-0000-0000-0000-000000000001/menu-items" | head -c 400 ; echo
```

### 3. Item B — live cross-service call, not mocked (2 pts)

`menu-service`'s `POST /restaurants/{rid}/menu-items` is owner-gated. The ownership check is **not local** — `menu-service` calls `restaurant-service` over HTTP at runtime (`RestaurantOwnershipClient` → `GET http://restaurant-service:8081/restaurants/{rid}`) and compares the JWT subject to the returned `ownerId`.

Mint a JWT for the owner of `d0000001` (`userId = 00000000-…-0001`) using the bash helper, then create a menu item:

```bash
JWT_OWNER1=$(scripts/mint-jwt.sh owner-1)

# Positive case (correct owner): expect HTTP 201 Created.
curl -i -X POST "http://localhost:8080/restaurants/d0000001-0000-0000-0000-000000000001/menu-items" \
  -H "Authorization: Bearer $JWT_OWNER1" -H "Content-Type: application/json" \
  -d '{"name":"Item B verification","priceAmount":1.00,"priceCurrency":"EUR","category":"Main"}'
```

`scripts/mint-jwt.sh` produces an HS256 token signed with the dev secret pinned in `application.properties`. Profiles available: `owner-1`, `owner-2`, `owner-3`, `customer`, `admin`, or `custom <userUuid> <role>`. See `scripts/mint-jwt.sh --help`.

To prove the lookup is **real and not mocked**, mint a token for a *different* owner (one who does not own `d0000001`) and replay the same `POST`:

```bash
JWT_OWNER2=$(scripts/mint-jwt.sh owner-2)

curl -i -X POST "http://localhost:8080/restaurants/d0000001-0000-0000-0000-000000000001/menu-items" \
  -H "Authorization: Bearer $JWT_OWNER2" -H "Content-Type: application/json" \
  -d '{"name":"Item B negative","priceAmount":1.00,"priceCurrency":"EUR","category":"Main"}'
```

Expected response — the shared error envelope, status `403`:

```
HTTP/1.1 403 Forbidden
{"timestamp":"...","status":403,"error":"Forbidden","message":"Access denied","path":"/restaurants/d0000001-.../menu-items"}
```

The 403 body itself is intentionally generic (no actor or owner leaked to the client). The proof that the cross-service call ran lives in `menu-service`'s logs — tail them while you re-run the negative case:

```bash
docker compose logs menu-service | grep -E "ownership denial"
```

Expected line:

```
... WARN ... MenuService : ownership denial actor=00000000-...-0002 role=RestaurantOwner endpoint=POST /restaurants/d0000001-... restaurantId=d0000001-... ownerId=00000000-...-0001
```

`menu-service` does not store restaurant ownership locally. The `ownerId=...-0001` value in that WARN line could only have been learned via the live `GET http://restaurant-service:8081/restaurants/{id}` call performed by `RestaurantOwnershipClient.findOwnerId()` (`menu-service/src/main/java/ee/ut/esi/quickbite/menu/security/RestaurantOwnershipClient.java`).

A pre-baked Postman collection that automates the positive + negative + B2B + auth-negative cases (no Python, no manual minting) ships at `postman/QuickBite-CP2.postman_collection.json`. Import it into Postman, run the collection, and watch every assertion go green against a fresh stack.

### 4. Item C — initial frontend displays real data (2 pts)

Open **http://localhost:8090** in a browser:

- Click **Restaurants** in the top nav. The view calls `GET /restaurants` through the gateway; the page renders the 6 seeded restaurants from `restaurant-db`.
- Click into any restaurant card. The detail view calls `GET /restaurants/{id}` and `GET /restaurants/{id}/menu-items`; the page renders the restaurant plus its menu (from `menu-db`).

Open the browser dev-tools Network tab to confirm the calls go to `http://localhost:8080/...` (the gateway) and return live JSON.

### 4b. Mike-Alfa (notification-service + Kafka event broker)

Owner: Melih Arık. Component 1 is the `notification-service` (A3 §3.7);
Component 2 is the Kafka event broker configuration (A3 §3.9 / §6.2),
which replaces the design-only Review Service. The compose stack adds
three containers: `kafka` (KRaft, auto-creates topics), `notification-db`
(PostgreSQL), and `notification-service` (Spring Boot, port 8087).

**Item A — second component runs (Event Broker + notification persistence).**

```bash
# Inspect topics declared by notification-service via KafkaAdmin
docker exec quickbite-kafka-1 rpk topic list --brokers kafka:9092

# Notification REST surface (Swagger UI):
#   http://localhost:8087/swagger-ui.html
```

Expected topics: `payment-events`, `delivery-events`, `order-events`,
`notification-events.DLQ`.

**Item B — real Kafka publish/consume between two services.**

The `notification-service` consumes `payment-events` and turns each event
into a `Notification` row. `POST /admin/publish-event` produces an event
to the broker exactly as Ege's `payment-service` will once its publisher
stub is swapped to a real producer (CP3). Round-trip proof:

```bash
RECIP="11111111-1111-1111-1111-111111111111"

# 1. Produce a payment.completed event to Kafka (real network call).
curl -s -X POST http://localhost:8087/admin/publish-event \
  -H 'Content-Type: application/json' \
  -d "{\"topic\":\"payment-events\",\"type\":\"payment.completed\",\
       \"payload\":{\"recipientId\":\"$RECIP\",\"orderId\":\"o-1\",\"message\":\"Payment confirmed\"}}"

# 2. notification-service consumes it asynchronously and persists a row.
sleep 2
curl -s "http://localhost:8087/notifications/unread-count" -H "X-User-Id: $RECIP"
# {"unreadCount":1}
```

The producer (`AdminController` → `KafkaTemplate`) and the consumer
(`KafkaEventConsumer.@KafkaListener`) live in different Spring contexts
within the same image but communicate strictly through the Kafka broker
container; nothing is mocked.

**Item C — frontend page consumes the notification-service.**

Open **http://localhost:8090/notifications**. The page prompts for a
user UUID, calls `GET /api/notifications` and `GET /api/notifications/unread-count`
through the gateway, and renders the live inbox. Use the same UUID you
just produced an event for to see the round-trip end-to-end in the UI.

### 5. Tear down

```bash
docker compose down
```

For a pristine restart (drops the seeded volumes so the next `up` re-runs Flyway `V1__init.sql` + `V2__seed_demo_data.sql` against empty databases):

```bash
docker compose down -v
```

Use `-v` between rehearsal runs if you want each demo to start from the canonical 6-restaurant / 16-item seed; omit it to keep menu items added during a previous rehearsal.


