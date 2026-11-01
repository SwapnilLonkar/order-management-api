# Order Management API

A REST API for managing customers, products, and orders — built as a portfolio
project to practice a clean, layered Spring Boot application.

## Problem statement

Small businesses need a simple way to keep track of who their customers are,
what they sell, and what has been ordered — including stock levels and the
lifecycle of each order. This API covers that core workflow:

- Register and manage customers.
- Maintain a product catalog with prices and stock quantities.
- Place orders for a customer, with one or more line items per order.
- Deduct stock when an order is placed, and reject the order when there is not
  enough stock (`InsufficientStockException`).
- Move an order through its lifecycle — `PENDING`, `CONFIRMED`, `SHIPPED`,
  `DELIVERED`, `CANCELLED` — with terminal states (`DELIVERED`, `CANCELLED`)
  guarded against further status changes.

## Architecture

Layered package structure under `com.example.OMS`:

```
src/main/java/com/example/OMS/
├── controller/   # REST endpoints; thin, delegates to services
├── service/      # Business logic (stock deduction, status guards)
├── repository/   # Spring Data JPA repositories
├── entity/       # JPA entities
├── dto/          # API request/response objects (validation lives here)
├── mapper/       # Hand-written @Component mappers (entity <-> DTO, via builders)
└── exception/    # Domain exceptions + a global exception handler
```

Design notes:

- Controllers stay thin; business rules live in the service layer.
- DTOs are the API contract, kept separate from JPA entities, so the database
  shape and the API shape can evolve independently.
- Validation annotations live on DTOs only, never on entities.
- Entities use Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`,
  `@AllArgsConstructor`, `@Builder`) and `@ManyToOne(fetch = FetchType.LAZY)`
  associations.
- A global exception handler maps domain exceptions (e.g. `CustomerNotFoundException`,
  `ProductNotFoundException`, `OrderNotFoundException`,
  `InsufficientStockException`) to appropriate HTTP responses.

## Tech stack

| Technology | Why |
| --- | --- |
| Java 17 | LTS baseline for the project |
| Spring Boot 4.1.0 | Application framework and dependency management |
| Spring Web MVC | REST controllers |
| Spring Data JPA + Hibernate | Persistence, with `ddl-auto: update` during development |
| PostgreSQL | Relational database, run locally via Docker Compose |
| Bean Validation | Request validation on DTOs |
| Lombok | Less boilerplate on entities and DTOs |
| springdoc-openapi 3.1.1 | Interactive API docs (Swagger UI) |
| Maven (wrapper) | Build tool; `./mvnw` needs no local Maven install |

## Local run

Prerequisites: Java 17 and Docker.

1. Start PostgreSQL:

   ```bash
   docker-compose up -d
   ```

   This starts a `postgres` container on port `5432` with database `orderdb`
   (user/password `postgres`/`postgres`, matching `src/main/resources/application.yml`).

2. Run the application:

   ```bash
   ./mvnw spring-boot:run
   ```

3. The API is available at <http://localhost:8080>. A welcome page is served
   at `/`, and interactive API documentation is available at
   <http://localhost:8080/swagger-ui.html>.

To stop the database:

```bash
docker-compose down
```

## Endpoint overview

All list endpoints support pagination via Spring Data `Pageable`
(`page`, `size`, `sort` query parameters).

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/` | Welcome page |
| POST | `/api/customers` | Create a customer |
| GET | `/api/customers/{id}` | Get a customer by id |
| GET | `/api/customers` | List customers (paged) |
| PUT | `/api/customers/{id}` | Update a customer |
| DELETE | `/api/customers/{id}` | Delete a customer |
| POST | `/api/products` | Create a product |
| GET | `/api/products/{id}` | Get a product by id |
| GET | `/api/products` | List products (paged) |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |
| POST | `/api/orders` | Create an order (deducts stock) |
| GET | `/api/orders/{id}` | Get an order by id |
| GET | `/api/orders` | List orders (paged) |
| GET | `/api/orders/customer/{customerId}` | List a customer's orders |
| PATCH | `/api/orders/{id}/status?status=SHIPPED` | Update an order's status |

## Roadmap

- Unit tests for `CustomerService` and `OrderService`.
- Spring Security / JWT authentication (next phase).
