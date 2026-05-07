# Menu Service

Sierra-Lima's Spring Boot service that owns the `MenuItem` aggregate
(requirements **R21** add/update/remove menu items and **R22** browse
menu).

Owner: Sierra-Lima.

## Layout

```
menu-service/
  pom.xml
  Dockerfile
  .dockerignore
  src/
    main/
      java/ee/ut/esi/quickbite/menu/
        MenuServiceApplication.java
        controller/                  HTTP entry points
        service/                     business logic + ownership checks
        repository/                  Spring Data JPA
        domain/                      JPA entities
        dto/                         request/response + validation
        config/                      SecurityConfig, OpenApiConfig
        security/                    JwtAuthFilter, RestaurantOwnershipClient
        events/                      MenuEventPublisher (log-only by default)
        exception/                   GlobalExceptionHandler + custom exceptions
      resources/
        application.properties           default profile
        application-docker.properties    overrides DB_URL to container hostname
        db/migration/                    Flyway V1__init.sql, V2__seed_demo_data.sql
    test/
      java/ee/ut/esi/quickbite/menu/     47 tests
```

## Responsibilities

- Owns the `MenuItem` aggregate root (name, description, price,
  category, availability flag, `restaurantId` reference by UUID).
- Serves the W1 batch validation `POST /menu-items/validate`.
- Serves browse routes (R22) and owner-gated CRUD (R21).
- Persists to a service-local PostgreSQL database (`menu-db`).
- Does **not** store restaurant metadata -- that belongs to
  `restaurant-service`. Cross-service references are by UUID only,
  no foreign keys.
- Optional: publishes `menu-events` on availability change. Default
  implementation is log-only; the Kafka swap is a one-class drop-in.

## API surface

| Method | Path | Who | Notes |
|---|---|---|---|
| `POST` | `/restaurants/{rid}/menu-items` | Owner of `{rid}` / Admin | Create. 201 on success. |
| `GET` | `/restaurants/{rid}/menu-items` | Public | Browse (R22). |
| `GET` | `/menu-items/{id}` | Public | Fetch by id. |
| `PUT` | `/menu-items/{id}` | Owner of parent restaurant / Admin | Update. |
| `DELETE` | `/menu-items/{id}` | Owner of parent restaurant / Admin | Remove. 204 on success. |
| `POST` | `/menu-items/validate` | Any authenticated | W1 batch validation. |

The optional `menu.item-availability-changed` Kafka producer is a
stretch, not baseline scope.

## Run locally

```bash
# Unit + integration tests (47 tests)
cd menu-service
mvn clean test
```

JWT auth (issuer-pinned HS256) is wired in
`security/JwtAuthFilter.java`. Restaurant ownership is checked via
`security/RestaurantOwnershipClient.java`, which calls the **public**
`GET /restaurants/{id}` on `restaurant-service` -- no token required
for that call.

## For AI coding agents

- **Ownership checks go in the service layer**, not the controller.
  See `service/MenuService.java` for the pattern: fetch the parent
  restaurant id, delegate to `RestaurantOwnershipClient`, throw
  `ForbiddenException` on mismatch.
- **Errors flow through `GlobalExceptionHandler`.** Custom exceptions
  in `exception/` each map to one canonical status code. Do not
  `throw new ResponseStatusException` from controllers.
- **Flyway migrations are append-only.** Add `V3__<name>.sql`; do not
  edit `V1__init.sql` or `V2__seed_demo_data.sql`.
- **The log-only publisher is intentional.** Do not swap in a Kafka
  implementation without a documented reason -- the baseline stance
  is log-only.
