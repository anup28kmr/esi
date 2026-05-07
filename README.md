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

**Prerequisites:** Docker Desktop running, plus Python 3 on PATH (only used by the Item B JWT-mint snippet). Windows users: please run the shell snippets below in Git Bash or WSL.

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

Mint a JWT for the owner of `d0000001` (`userId = 00000000-…-0001`) and create a menu item:

```bash
JWT=$(python -c "
import base64, hmac, hashlib, json, time
secret = base64.b64decode('dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY=')
hdr = {'alg':'HS256','typ':'JWT'}
now = int(time.time())
pld = {'iss':'quickbite-user-service','sub':'00000000-0000-0000-0000-000000000001','userId':'00000000-0000-0000-0000-000000000001','role':'RestaurantOwner','tokenType':'USER','iat':now,'exp':now+3600}
def b64u(b): return base64.urlsafe_b64encode(b).rstrip(b'=')
si = b64u(json.dumps(hdr,separators=(',',':')).encode()) + b'.' + b64u(json.dumps(pld,separators=(',',':')).encode())
sig = hmac.new(secret, si, hashlib.sha256).digest()
print((si + b'.' + b64u(sig)).decode())
")

# Positive case (correct owner): expect HTTP 201 Created.
curl -i -X POST "http://localhost:8080/restaurants/d0000001-0000-0000-0000-000000000001/menu-items" \
  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d '{"name":"Item B verification","priceAmount":1.00,"priceCurrency":"EUR","category":"Main"}'
```

To prove the lookup is **real and not mocked**, repeat the above with `userId` and `sub` ending in `-0002` instead of `-0001` (a different user, who owns other restaurants but not `d0000001`):

```
HTTP/1.1 403 Forbidden
{"message":"User 00000000-0000-0000-0000-000000000002 does not own restaurant d0000001-0000-0000-0000-000000000001", ...}
```

The `ownerId` referenced in that error message could only have been learned by a live HTTP call to `restaurant-service` — `menu-service` does not store restaurant ownership locally. To see the call in action, tail the logs while you re-run the negative case:

```bash
docker compose logs -f menu-service | grep -E "ownership|denial"
```

### 4. Item C — initial frontend displays real data (2 pts)

Open **http://localhost:8090** in a browser:

- Click **Restaurants** in the top nav. The view calls `GET /restaurants` through the gateway; the page renders the 6 seeded restaurants from `restaurant-db`.
- Click into any restaurant card. The detail view calls `GET /restaurants/{id}` and `GET /restaurants/{id}/menu-items`; the page renders the restaurant plus its menu (from `menu-db`).

Open the browser dev-tools Network tab to confirm the calls go to `http://localhost:8080/...` (the gateway) and return live JSON.

### 5. Tear down

```bash
docker compose down
```


