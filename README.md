# E-Commerce Distributed System
[![CI - Spring Boot Microservices](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)
**Microservices architecture with Spring Boot**

## Overview
This project implements a distributed e-commerce system based on a microservices architecture using Spring Boot.
Each business domain is isolated into its own service, allowing independent deployment, scalability, and maintenance.

Services are registered through **Eureka Service Discovery** and exposed externally via an **API Gateway**. Communication between services that require business consistency (e.g., order processing and stock control) is handled asynchronously using **RabbitMQ**.

Each service manages its own database following the **database-per-service** pattern.

## Architecture
The system is composed of independent services responsible for different business domains:

- Service Discovery for dynamic service registration and lookup
- API Gateway for centralized routing and entry point
- Authentication service for identity and access control
- Product service for catalog and stock management
- Customer service for customer data
- Order service for order lifecycle
- Payment service for payment processing

All services are built using Spring Boot and communicate through REST when necessary, or through asynchronous messaging for business events.

## Business Flow
Order processing follows an event-driven approach to maintain consistency between distributed services:

1. A client creates an order through the API Gateway
2. The order-service persists the order and publishes an **OrderCreated** event
3. The payment-service consumes the event and processes the payment request
4. When the payment is approved, a **PaymentApproved** event is published
5. The product-service consumes the event and updates the stock of the purchased products

This asynchronous workflow reduces coupling between services and allows each component to react to business events independently.

## Security
Authentication and authorization are handled by the auth-service using Spring Security and JWT.
The API Gateway validates incoming requests and ensures that only authenticated users can access protected endpoints across the system.

### API Rate Limiting (Production-ready)
The API Gateway enforces distributed rate limiting backed by Redis:

- **Auth endpoints (`/api/auth/**`)**: limit by **client IP** (anti-brute-force)
- **Business endpoints (`/api/products/**`, `/api/customers/**`, `/api/orders/**`, `/api/payments/**`)**: limit by **authenticated user** (JWT subject)
- Uses token-bucket algorithm through Spring Cloud Gateway `RequestRateLimiter` + Redis
- Works across multiple gateway instances (shared counters in Redis)
- User policy is intentionally simple: **1 request = 1 token** and the bucket refills at **100 tokens per minute**

## Technologies
- Java 17
- Spring Boot
- Spring Cloud Eureka
- Spring Cloud Gateway
- Spring Security
- JWT
- Spring Data JPA
- MySQL
- RabbitMQ
- Redis
- Docker
- Docker Compose
- Maven

## Services
| Service | Port | Responsibility |
| --- | --- | --- |
| discovery-service | 8761 | Service registry |
| api-gateway | 8080 | API routing and entry point |
| auth-service | 8082 | Authentication and authorization |
| product-service | 8081 | Product catalog and stock control |
| customer-service | 8083 | Customer management |
| order-service | 8084 | Order processing |
| payment-service | 8085 | Payment processing |

## Running the Application
Start all services and infrastructure components using Docker Compose:

```
docker-compose up -d --build
```

After startup, the rate limit defaults are:

- Auth: `20 req/s` with burst `40` per IP
- Business APIs: `100 req/min` per user (`replenishRate=100`, `requestedTokens=1`, `burst=100`)

Customize in `.env`:

- `RATE_LIMIT_AUTH_REPLENISH_RATE`
- `RATE_LIMIT_AUTH_BURST_CAPACITY`
- `RATE_LIMIT_USER_REPLENISH_RATE`
- `RATE_LIMIT_USER_BURST_CAPACITY`
- `RATE_LIMIT_USER_REQUESTED_TOKENS`

### Quick test

Use the same bearer token and send 101 requests in under a minute:

```bash
TOKEN="<access_token>"
for i in $(seq 1 101); do
	code=$(curl -s -o /dev/null -w "%{http_code}" \
		-H "Authorization: Bearer $TOKEN" \
		http://localhost:8080/api/products)
	echo "$i -> $code"
done
```

Expected result:

- Requests 1-100: `200` or the upstream service response
- Request 101: `429 Too Many Requests`

To stop the environment:

```
docker-compose down
```

## Notes
The system follows basic principles of service isolation and separation of responsibilities.
Asynchronous communication is used where eventual consistency is required between domains such as orders, payments, and inventory.