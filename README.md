# QuickBite — Microservices Food Delivery Platform

QuickBite is a modern, microservices-based food delivery platform built with Spring Boot 4 and Vue.js 3. It demonstrates enterprise service integration patterns, distributed data management, and containerized deployment.

---

## Architecture

The system consists of several specialized microservices and a central API Gateway. All client traffic (Frontend or external API calls) flows through the Gateway.

```
Frontend (Vue.js, port 8090)
  |
  v
API Gateway (port 9090) --+--> /api/auth, /api/users     --> User Service (port 7000)
                          +--> /api/orders               --> Order Service (port 7001)
                          +--> /api/restaurants          --> Restaurant Service (port 8081)
                          +--> /api/menu-items           --> Menu Service (port 8082)
                          +--> /api/payments             --> Payment Service (port 8085)
                          +--> /api/deliveries           --> Delivery Service (port 8086)
                          +--> (Future: /api/notify)      --> Notification Service (port 8087)
```

### Services & Ports

| Service               | Internal Port | External Port | Description                                      |
|-----------------------|---------------|---------------|--------------------------------------------------|
| **api-gateway**       | -             | 9090          | Central entry point; routing and cross-cutting concerns |
| **frontend**          | -             | 8090          | Vue.js 3 Web Dashboard                           |
| **user-service**      | 7000          | 7000          | User management, auth, and address profiles      |
| **order-service**     | 7001          | 7001          | Order lifecycle and status management            |
| **restaurant-service**| 8081          | 8081          | Restaurant directory and availability            |
| **menu-service**      | 8082          | 8082          | Menu items, categories, and pricing              |
| **payment-service**   | 8085          | 8085          | Payment processing (Stripe integration/mock)     |
| **delivery-service**  | 8086          | 8086          | Courier assignment and tracking                  |
| **notification-svc**  | 8087          | 8087          | Email/Push notifications (Background)            |

---

## Tech Stack

- **Backend:** Java 21, Spring Boot 4.0.6
- **Gateway:** Spring Cloud Gateway (WebMVC variant)
- **Frontend:** Vue.js 3, Vue Router, Axios
- **Database:** PostgreSQL 15/16 (service-specific databases)
- **Migrations:** Flyway (User Service)
- **Infrastructure:** Docker & Docker Compose
- **Documentation:** SpringDoc OpenAPI 3 (Swagger UI)
- **Utilities:** Lombok, MapStruct

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- JDK 21+ (for local development)
- Maven 3.9+ (for local development)
- Node.js 18+ & npm (for frontend development)

---

## Getting Started

### Running with Docker (Recommended)

Start the entire stack (services and databases):
```bash
docker compose up -d
```

Access the applications:
- **Web UI:** `http://localhost:8090`
- **API Gateway:** `http://localhost:9090/api/...`

### Local Development

#### Backend (Spring Boot)
Each service can be run independently. Ensure its respective PostgreSQL container is running via Docker.
```bash
cd <service-directory>
./mvnw spring-boot:run
```

#### Frontend (Vue.js)
```bash
cd frontend
npm install
npm run serve
```
The frontend will be available at `http://localhost:8090`.

---

## Project Structure

```
esi/
├── api-gateway/          # Spring Cloud Gateway (MVC)
├── delivery-service/     # Delivery & Courier logic
├── frontend/             # Vue.js 3 SPA
├── menu-service/         # Menu management
├── notification-service/ # Notifications (Email/SMS)
├── order-service/        # Order processing
├── payment-service/      # Payment handling
├── restaurant-service/   # Restaurant metadata
├── user-service/         # Authentication & Users
├── scripts/              # Utility scripts (JWT minting, etc)
└── docker-compose.yml    # Infrastructure orchestration
```

---

## API Documentation

Each service provides a Swagger UI for API exploration:

- **User Service:** `http://localhost:7000/swagger-ui.html`
- **Order Service:** `http://localhost:7001/swagger-ui.html`
- **Restaurant Service:** `http://localhost:8081/swagger-ui.html`
- **Menu Service:** `http://localhost:8082/swagger-ui.html`

**Note:** When calling services through the Gateway (9090), use the `/api` prefix (e.g., `GET http://localhost:9090/api/users`).

---

## Scripts & Utilities

### JWT Minting
For development, you can generate valid JWT tokens for different roles using the provided script:
```bash
# Default (Owner-1)
./scripts/mint-jwt.sh

# Specific role
./scripts/mint-jwt.sh customer
./scripts/mint-jwt.sh admin
```

---

## Testing

Run tests for any backend service:
```bash
cd <service-directory>
./mvnw test
```

---

## Environment Variables

Key variables used in `docker-compose.yml`:

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Secret key for signing/verifying JWTs |
| `DB_HOST` | Database hostname (usually service name in Docker) |
| `SPRING_PROFILES_ACTIVE` | Set to `docker` for containerized config |

---

