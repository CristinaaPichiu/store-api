# Store API

A RESTful store application built with Spring Boot as the final project for the **Web Development with Java and Spring** module.

## Tech Stack

- **Java 17**
- **Spring Boot 3.5**
- **Spring Security** — session-based authentication
- **Spring Session JDBC** — persistent sessions in PostgreSQL
- **Spring Data JPA / Hibernate** — ORM
- **PostgreSQL** — primary database
- **Springdoc OpenAPI** — Swagger UI
- **Testcontainers** — integration tests with real PostgreSQL
- **JaCoCo** — code coverage
- **Docker Compose** — local database setup
- **Lombok** — boilerplate reduction

---

## Features

| # | Endpoint | Description |
|---|----------|-------------|
| 1 | `POST /api/auth/register` | Register a new user |
| 2 | `POST /api/auth/login` | Login and receive a session ID |
| 3 | `GET /api/products` | List all available products |
| 4 | `POST /api/cart` | Add a product to cart |
| 5 | `GET /api/cart` | View cart contents with subtotal |
| 6 | `PUT /api/cart/{cartItemId}` | Modify cart item quantity |
| 7 | `DELETE /api/cart/{cartItemId}` | Remove item from cart |
| 8 | `POST /api/cart/checkout` | Checkout and place order |

### Security
- Passwords are stored as **BCrypt hashes** — never plain text
- **Brute-force protection**: account locked for 15 minutes after 5 failed login attempts
- Cart is **session-scoped**: each session has its own isolated cart, cleared on checkout or expiry

---

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose

### 1. Configure environment

Create a `.env` file in the project root 


### 2. Start the database

```bash
docker-compose up -d
```

### 3. Run the application

```bash
./gradlew bootRun
```

The app will start on `http://localhost:8080`.

The database schema is created automatically by Hibernate on first run, and sample products are seeded via `data.sql`.

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

---

## Running Tests

**Unit tests:**
```bash
./gradlew test
```

**Integration tests** (requires Docker for Testcontainers):
```bash
./gradlew integrationTest
```

**All tests with coverage report:**
```bash
./gradlew test integrationTest jacocoTestReport
```

Coverage report: `build/reports/jacoco/test/html/index.html`

---

## Postman Collection

A ready-to-use Postman collection is available at `utils/Store API.postman_collection.json`.

It covers all endpoints organized into three folders:

- **Auth** — Register, Login
- **Products** — Get All Products
- **Cart** — Add to Cart, Get Cart, Modify Cart Item, Remove Cart Item, Checkout
  To use it, import the file into Postman and set the `baseUrl` variable to `http://localhost:8080`.

> **Note:** Login first to obtain a session cookie — subsequent cart requests require an active session.
 
---

## Project Structure

```
src/main/java/com/example/storeapi/
├── controller/       # REST controllers (Auth, Cart, Product)
├── service/          # Business logic
├── model/            # JPA entities (User, Product, CartItem, Order, OrderItem)
├── repository/       # Spring Data repositories
├── dto/              # Request/Response DTOs
├── exception/        # Custom exceptions + GlobalExceptionHandler
└── config/           # Security and Swagger configuration

integration-tests/    # Integration tests using Testcontainers
```