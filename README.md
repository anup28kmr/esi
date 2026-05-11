# ESI — Enterprise System Integration

A microservices-based food delivery platform built with Spring Boot 4, demonstrating enterprise service integration patterns. The system is composed of three services: an API Gateway, a User Service, and an Order Service.

---

## Architecture

```
Client
  |
  v
API Gateway (port 9090)
  |
  +---> User Service (port 7000) ---> PostgreSQL (port 5433)
  |
  +---> Order Service (port 7001) ---> PostgreSQL (port 5432)
```

All client requests flow through the API Gateway, which routes them to the appropriate downstream service based on the URL path.

| Service       | Port | Description                                      |
|---------------|------|--------------------------------------------------|
| api-gateway   | 9090 | Single entry point; routes requests to services  |
| user-service  | 7000 | User registration, authentication, addresses     |
| order-service | 7001 | Order placement, tracking, status management     |
| postgres-user | 5433 | PostgreSQL database for user-service             |
| postgres-order| 5432 | PostgreSQL database for order-service            |

---

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.6**
- **Spring WebFlux** (api-gateway)
- **Spring Web MVC** (user-service, order-service)
- **Spring Data JPA / Hibernate**
- **Flyway** (database migrations for user-service)
- **PostgreSQL 15**
- **Docker & Docker Compose**
- **SpringDoc OpenAPI 3 (Swagger UI)**
- **Lombok**

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- (For local development) JDK 21, Maven 3.9+

---

## Running with Docker

### Start all services

```bash
docker compose up -d
```

### Stop all services

```bash
docker compose down
```

### Rebuild a specific service after code changes

```bash
docker compose up -d --build <service-name>
# e.g.
docker compose up -d --build user-service
```

### View logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f user-service
```

---

## API Gateway — Routing

All requests should be sent to the API Gateway on port **9090**. The gateway routes based on path prefix:

| Path Prefix        | Routes To     |
|--------------------|---------------|
| `/auth/**`         | user-service  |
| `/users/**`        | user-service  |
| `/driver-profiles/**` | user-service |
| `/orders/**`       | order-service |

---

## API Reference

### User Service

Base URL (via gateway): `http://localhost:9090`
Base URL (direct): `http://localhost:7000`
Swagger UI: `http://localhost:7000/swagger-ui.html`

---

#### Authentication

##### Login
```
POST /auth/login
```
Request body:
```json
{
  "email": "user@example.com",
  "password": "secret"
}
```
Response `200 OK`:
```json
{
  "token": "<jwt-or-session-token>",
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "fullName": "John Doe",
    "phoneNumber": "+372 5000 0000",
    "role": "CUSTOMER",
    "status": "ACTIVE"
  }
}
```
Response `401 Unauthorized`:
```json
{
  "apiPath": "/auth/login",
  "errorCode": "UNAUTHORIZED",
  "errorMessage": "Invalid credentials",
  "errorTime": "2026-05-08T10:00:00"
}
```

---

#### Users

##### Register User
```
POST /users
```
Request body:
```json
{
  "email": "user@example.com",
  "password": "secret",
  "fullName": "John Doe",
  "phoneNumber": "+372 5000 0000",
  "role": "CUSTOMER"
}
```
Available roles: `CUSTOMER`, `DRIVER`, `RESTAURANT_OWNER`, `ADMIN`

Response `201 Created`: `UserDTO`

---

##### Get User Profile
```
GET /users/{id}
```
Response `200 OK`:
```json
{
  "userId": 1,
  "email": "user@example.com",
  "fullName": "John Doe",
  "phoneNumber": "+372 5000 0000",
  "role": "CUSTOMER",
  "status": "ACTIVE"
}
```
Response `404 Not Found`: `ErrorResponseDTO`

---

##### Update User Profile
```
PUT /users/{id}
```
Request body: `UserDTO` (same structure as register)

Response `200 OK`: updated `UserDTO`

---

##### List User Addresses
```
GET /users/{id}/addresses
```
Response `200 OK`:
```json
[
  {
    "addressId": 1,
    "street": "Narva mnt 18",
    "city": "Tartu",
    "postalCode": "51009",
    "label": "Home",
    "isDefault": true
  }
]
```

---

##### Add User Address
```
POST /users/{id}/addresses
```
Request body:
```json
{
  "street": "Narva mnt 18",
  "city": "Tartu",
  "postalCode": "51009",
  "label": "Home",
  "isDefault": true
}
```
Response `201 Created`: `AddressDTO`

---

#### Driver Profiles

##### List Available Drivers
```
GET /driver-profiles?available=true
```
Query parameters:

| Parameter   | Type    | Default | Description              |
|-------------|---------|---------|--------------------------|
| `available` | boolean | `true`  | Filter by availability   |

Response `200 OK`:
```json
[
  {
    "userId": 5,
    "licenseNumber": "AB12345",
    "vehicleType": "CAR",
    "available": true
  }
]
```

---

### Order Service

Base URL (via gateway): `http://localhost:9090`
Base URL (direct): `http://localhost:7001`
Swagger UI: `http://localhost:7001/swagger-ui.html`

---

##### Place Order
```
POST /orders
X-User-Id: <customer-id>
```
The `X-User-Id` header is required and identifies the authenticated customer.

Request body:
```json
{
  "restaurantId": "restaurant-123",
  "items": [
    {
      "menuItemId": "item-1",
      "name": "Margherita Pizza",
      "unitPrice": 9.99,
      "quantity": 2
    }
  ]
}
```
Response `201 Created`:
```json
{
  "orderId": 1,
  "customerId": 42,
  "restaurantId": "restaurant-123",
  "status": "PENDING",
  "totalAmount": 19.98,
  "items": [
    {
      "menuItemId": "item-1",
      "name": "Margherita Pizza",
      "unitPrice": 9.99,
      "quantity": 2
    }
  ],
  "paymentStarted": false,
  "deliveryTaskCreated": false
}
```

---

##### Get Order
```
GET /orders/{id}
```
Response `200 OK`: `OrderResponse`
Response `404 Not Found`: `ErrorResponse`

---

##### List Orders by Customer
```
GET /orders?customerId={customerId}
```
Response `200 OK`: `List<OrderResponse>`

---

##### Get Order Items
```
GET /orders/{id}/items
```
Response `200 OK`:
```json
[
  {
    "menuItemId": "item-1",
    "name": "Margherita Pizza",
    "unitPrice": 9.99,
    "quantity": 2
  }
]
```

---

##### Update Order Status
```
PATCH /orders/{id}/status
```
Request body:
```json
{
  "status": "CONFIRMED"
}
```
Response `200 OK`: updated `OrderResponse`

---

##### Cancel Order
```
DELETE /orders/{id}
```
Response `204 No Content`
Response `400 Bad Request` if the order cannot be cancelled (e.g. already delivered)

---

## Environment Variables

### user-service

| Variable    | Default       | Description                   |
|-------------|---------------|-------------------------------|
| `DB_HOST`   | `localhost`   | PostgreSQL host               |
| `DB_PORT`   | `5432`        | PostgreSQL port               |
| `DB_NAME`   | `user_service`| Database name                 |
| `DB_USERNAME` | `postgres`  | Database username             |
| `DB_PASSWORD` | `Postgres.2026` | Database password         |

### order-service

| Variable      | Default        | Description                   |
|---------------|----------------|-------------------------------|
| `DB_HOST`     | `localhost`    | PostgreSQL host               |
| `DB_PORT`     | `5432`         | PostgreSQL port               |
| `DB_NAME`     | `order_service`| Database name                 |
| `DB_USERNAME` | `postgres`     | Database username             |
| `DB_PASSWORD` | `password`     | Database password             |

### api-gateway

| Variable             | Default      | Description                  |
|----------------------|--------------|------------------------------|
| `USER_SERVICE_HOST`  | `localhost`  | user-service hostname        |
| `USER_SERVICE_PORT`  | `7000`       | user-service port            |
| `ORDER_SERVICE_HOST` | `localhost`  | order-service hostname       |
| `ORDER_SERVICE_PORT` | `7001`       | order-service port           |

---

## Database Schema

### user-service (Flyway-managed)

**`users`**

| Column          | Type        | Constraints                              |
|-----------------|-------------|------------------------------------------|
| `user_id`       | BIGINT      | PK                                       |
| `email`         | VARCHAR     | NOT NULL, UNIQUE                         |
| `full_name`     | VARCHAR     |                                          |
| `password_hash` | VARCHAR     |                                          |
| `phone_number`  | VARCHAR     |                                          |
| `role`          | VARCHAR     | CUSTOMER / DRIVER / RESTAURANT_OWNER / ADMIN |
| `status`        | VARCHAR     | ACTIVE / SUSPENDED                       |
| `created_at`    | TIMESTAMP   |                                          |

**`address`**

| Column        | Type    | Constraints       |
|---------------|---------|-------------------|
| `address_id`  | BIGINT  | PK                |
| `street`      | VARCHAR |                   |
| `city`        | VARCHAR |                   |
| `postal_code` | VARCHAR |                   |
| `label`       | VARCHAR |                   |
| `is_default`  | BOOLEAN |                   |
| `user_id`     | BIGINT  | FK → users        |

**`driver_profile`**

| Column           | Type    | Constraints       |
|------------------|---------|-------------------|
| `user_id`        | BIGINT  | PK, FK → users    |
| `license_number` | VARCHAR |                   |
| `vehicle_type`   | VARCHAR |                   |
| `is_available`   | BOOLEAN |                   |

### order-service (Hibernate-managed)

**`orders`**

| Column                  | Type       | Description                  |
|-------------------------|------------|------------------------------|
| `order_id`              | BIGINT     | PK                           |
| `restaurant_id`         | VARCHAR    |                              |
| `status`                | VARCHAR    |                              |
| `total_amount`          | DECIMAL    |                              |
| `payment_started`       | BOOLEAN    |                              |
| `delivery_task_created` | BOOLEAN    |                              |
| `user_user_id`          | BIGINT     | FK → order_users             |

**`order_item`**

| Column        | Type    | Description           |
|---------------|---------|-----------------------|
| `item_id`     | BIGINT  | PK                    |
| `menu_item_id`| VARCHAR |                       |
| `name`        | VARCHAR |                       |
| `unit_price`  | DECIMAL |                       |
| `quantity`    | INTEGER |                       |
| `order_id`    | BIGINT  | FK → orders           |

**`order_users`**

| Column      | Type    | Description      |
|-------------|---------|------------------|
| `user_id`   | BIGINT  | PK               |
| `email`     | VARCHAR |                  |
| `full_name` | VARCHAR |                  |

---

## Project Structure

```
esi/
├── api-gateway/                  # API Gateway (Spring WebFlux)
│   ├── src/main/java/.../
│   │   ├── ApiGatewayApplication.java
│   │   └── GatewayFilter.java    # Path-based proxy routing
│   ├── src/main/resources/
│   │   └── application.yml
│   └── Dockerfile
│
├── user-service/                 # User Service (Spring MVC)
│   ├── src/main/java/.../
│   │   ├── controller/           # AuthController, UserController, DriverProfileController
│   │   ├── service/              # UserService interface + impl
│   │   ├── entity/               # User, Address, DriverProfile
│   │   ├── repository/           # JPA repositories
│   │   ├── dto/                  # Request/Response DTOs
│   │   ├── mapper/               # Entity <-> DTO mappers
│   │   └── exception/            # GlobalExceptionHandler + custom exceptions
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── db/migration/         # Flyway SQL migrations
│   └── Dockerfile
│
├── order-service/                # Order Service (Spring MVC)
│   ├── src/main/java/.../
│   │   ├── controller/           # OrderController
│   │   ├── service/              # OrderService interface + impl
│   │   ├── entity/               # Order, OrderItem, User
│   │   ├── repository/           # JPA repositories
│   │   ├── dto/                  # Request/Response DTOs
│   │   ├── mapper/               # Entity <-> DTO mappers
│   │   └── exception/            # GlobalExceptionHandler + custom exceptions
│   ├── src/main/resources/
│   │   └── application.yaml
│   └── Dockerfile
│
├── db/
│   └── init/                     # PostgreSQL init scripts
│       ├── 01-create-databases-user.sql
│       └── 01-create-databases-order.sql
│
└── docker-compose.yml
```
