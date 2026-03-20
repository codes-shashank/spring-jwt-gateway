# Spring Boot API Gateway + JWT Security Demo

A multi-module Spring Boot project demonstrating **API Gateway** (Spring Cloud Gateway)
with **Spring Security** and **JWT authentication/authorization**.

---

## Architecture

```
Client
  │
  ▼
┌────────────────────────┐
│   API Gateway (:8080)  │   ← Spring Cloud Gateway
│  JwtAuthenticationFilter│   ← Validates JWT on all protected routes
│  RoleAuthorizationFilter│   ← Checks role for /api/admin/**
│  RequestLoggingFilter  │   ← Global logging (all routes)
└─────────┬──────────────┘
          │ routes (with X-Auth-Username / X-Auth-Role headers)
    ┌─────┴──────┐
    ▼            ▼
┌──────────┐  ┌────────────────┐
│   Auth   │  │  User Service  │
│ Service  │  │   (:8082)      │
│  (:8081) │  │  /api/users/** │
│ /auth/** │  │  /api/admin/** │
└──────────┘  └────────────────┘
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **api-gateway** | 8080 | Routes all requests, validates JWT, enforces roles |
| **auth-service** | 8081 | Issues and validates JWT tokens, manages users |
| **user-service** | 8082 | Business logic — user profiles and admin operations |

---

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+

### Build & Run

```bash
# 1. Clone and build all modules
mvn clean install -DskipTests

# 2. Start Auth Service (terminal 1)
cd auth-service
mvn spring-boot:run

# 3. Start User Service (terminal 2)
cd user-service
mvn spring-boot:run

# 4. Start API Gateway (terminal 3)
cd api-gateway
mvn spring-boot:run
```

All traffic goes through the gateway on port **8080**.

---

## API Endpoints

### Authentication (public — no JWT needed)

#### Register a new user
```http
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "username": "alice",
  "password": "password123",
  "role": "ROLE_USER"
}
```

#### Login and get JWT token
```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ROLE_ADMIN",
  "message": "Login successful"
}
```

#### Validate a token
```http
GET http://localhost:8080/auth/validate?token=eyJhbGciOiJIUzI1NiJ9...
```

---

### Protected Endpoints (JWT required)

Use the token from login as a Bearer token in the Authorization header.

#### Get my profile
```http
GET http://localhost:8080/api/users/profile
Authorization: Bearer <token>
```

#### Get all users
```http
GET http://localhost:8080/api/users
Authorization: Bearer <token>
```

#### Get user by ID
```http
GET http://localhost:8080/api/users/1
Authorization: Bearer <token>
```

---

### Admin Endpoints (ROLE_ADMIN required)

```http
GET    http://localhost:8080/api/admin/dashboard
GET    http://localhost:8080/api/admin/users
DELETE http://localhost:8080/api/admin/users/2
Authorization: Bearer <admin-token>
```

> Regular users (`ROLE_USER`) will receive **403 Forbidden** on these endpoints.

---

## Pre-seeded Test Users

The auth service seeds two users on startup:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | `ROLE_ADMIN` |
| `user` | `user123` | `ROLE_USER` |

---

## JWT Flow

```
1. Client POSTs credentials to POST /auth/login
2. Auth Service validates credentials → issues a signed JWT
   JWT payload:  { sub: "alice", role: "ROLE_USER", iat: ..., exp: ... }

3. Client sends: Authorization: Bearer <jwt>

4. API Gateway (JwtAuthenticationFilter):
   a. Extracts token from Authorization header
   b. Verifies HMAC-SHA256 signature using shared secret
   c. Checks expiration
   d. Extracts claims (username, role)
   e. Forwards as headers to downstream service:
      X-Auth-Username: alice
      X-Auth-Role: ROLE_USER

5. (For /api/admin/**) RoleAuthorizationFilter:
   a. Reads X-Auth-Role header
   b. Compares against requiredRole config (ROLE_ADMIN)
   c. Returns 403 if mismatch

6. User Service:
   a. JwtAuthFilter reads X-Auth-Username / X-Auth-Role (fast path)
   b. Sets Spring Security context
   c. @PreAuthorize / HttpSecurity rules can enforce further
```

---

## Security Layers

This demo implements **defense in depth** with multiple security layers:

1. **API Gateway** — First line of defense; rejects invalid/missing tokens before they reach any service
2. **Role filter** — Gateway also enforces role-based access per route
3. **User Service** — Has its own `JwtAuthFilter` as a second layer; doesn't blindly trust headers if called directly

---

## Configuration

All three services share the **same JWT secret** (in `application.yml`).
In production, this secret should be:
- Stored in a secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.)
- At least 256 bits / 32 characters
- Injected via environment variables

```yaml
jwt:
  secret: ${JWT_SECRET}  # Use environment variable in production
  expiration: 3600       # Token lifetime in seconds
```

---

## Project Structure

```
spring-jwt-gateway/
├── pom.xml                          ← Parent POM
│
├── api-gateway/                     ← Spring Cloud Gateway
│   └── src/main/java/com/example/
│       ├── ApiGatewayApplication.java
│       ├── config/
│       │   └── JwtValidator.java    ← Token parsing utility
│       └── filter/
│           ├── JwtAuthenticationFilter.java  ← Per-route JWT filter
│           ├── RoleAuthorizationFilter.java  ← Per-route role check
│           └── RequestLoggingFilter.java     ← Global logging
│
├── auth-service/                    ← Authentication microservice
│   └── src/main/java/com/example/
│       ├── AuthServiceApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   └── DataInitializer.java ← Seeds test users
│       ├── controller/
│       │   └── AuthController.java
│       ├── model/
│       │   ├── User.java
│       │   └── AuthModels.java
│       ├── repository/
│       │   └── UserRepository.java
│       ├── service/
│       │   ├── AuthService.java
│       │   └── CustomUserDetailsService.java
│       └── util/
│           └── JwtUtil.java
│
└── user-service/                    ← Business logic microservice
    └── src/main/java/com/example/
        ├── UserServiceApplication.java
        ├── config/
        │   ├── SecurityConfig.java
        │   └── JwtAuthFilter.java   ← Defense-in-depth JWT filter
        ├── controller/
        │   ├── UserController.java
        │   └── AdminController.java
        └── model/
            └── UserProfile.java
```

---

## Example: Full Login → Protected Request Flow

```bash
# Step 1: Login as admin
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"

# Step 2: Access user profile (any authenticated user)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/profile

# Step 3: Access admin dashboard (ROLE_ADMIN only)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/dashboard

# Step 4: Try admin endpoint with regular user (expect 403)
USER_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -H "Authorization: Bearer $USER_TOKEN" http://localhost:8080/api/admin/dashboard
# → {"error": "Access denied. Required role: ROLE_ADMIN"}
```

---

## H2 Database Console (Auth Service)

Available at: http://localhost:8081/h2-console

- **JDBC URL**: `jdbc:h2:mem:authdb`
- **Username**: `sa`
- **Password**: *(empty)*
