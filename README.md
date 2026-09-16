# Personal Finance Manager API

A comprehensive personal finance management system built with **Spring Boot 3.3.4** that enables users to track income, expenses, savings goals, and generate financial reports.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security (Session-based Authentication) |
| Database | H2 (file-based, zero-config) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5, Mockito |
| Coverage | JaCoCo |
| Build Tool | Maven |
| Deployment | Render |

## Features

- **User Authentication** — Register, login (session cookies), logout with complete data isolation
- **Transaction Management** — Full CRUD with date validation, category-based typing, and filtering
- **Category System** — 7 pre-seeded default categories + user-defined custom categories
- **Savings Goals** — Target-based goals with dynamic progress tracking (Income - Expenses)
- **Financial Reports** — Monthly and yearly analytics with category-wise breakdowns

## Design Decisions

### Why Session-Based Auth over JWT?
Session-based authentication with HTTP-only cookies provides better security for server-rendered or same-origin APIs — cookies are automatically managed by the browser/client, sessions can be revoked server-side instantly, and there's no need to manage token refresh logic.

### Why H2 over PostgreSQL?
H2 was chosen for zero-configuration deployment. The file-based mode persists data across restarts while requiring no external database setup — ideal for evaluation and Render free-tier hosting.

### Why BigDecimal for Financial Amounts?
`BigDecimal` avoids floating-point precision errors that `Double` introduces in financial calculations (e.g., `0.1 + 0.2 != 0.3` with doubles).

## Architecture

```
Controller → Service → Repository → Database
     ↑            ↑
  DTOs      Exception Handler (@ControllerAdvice)
```

**Layered Architecture** with strict separation of concerns:
- **Controllers** — HTTP request/response handling, validation
- **Services** — Business logic, authorization checks
- **Repositories** — Data access with custom JPQL queries
- **DTOs** — Separate request/response objects from entities
- **Global Exception Handler** — Consistent error responses with proper HTTP status codes

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Local Development

```bash
# Clone the repository
git clone https://github.com/Aaryav1130/personal-finance-management-system.git
cd personal-finance-management-system

# Run the application
./mvnw spring-boot:run

# Application starts at http://localhost:8080/api
```

### Run Tests

```bash
# Run all tests with coverage report
./mvnw test

# Coverage report generated at: target/site/jacoco/index.html
```

### Docker

```bash
docker build -t finance-manager .
docker run -p 8080:8080 finance-manager
```

## API Documentation

Base URL: `/api`

### 1. Authentication

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | Register a new user | 201 |
| POST | `/api/auth/login` | Login (returns session cookie) | 200 |
| POST | `/api/auth/logout` | Logout (invalidates session) | 200 |

**Register:**
```json
POST /api/auth/register
{
  "username": "user@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phoneNumber": "+1234567890"
}
// Response: { "message": "User registered successfully", "userId": 1 }
```

**Login:**
```json
POST /api/auth/login
{ "username": "user@example.com", "password": "password123" }
// Response: { "message": "Login successful" }
// Sets session cookie for subsequent requests
```

### 2. Transactions

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/transactions` | Create a transaction | 201 |
| GET | `/api/transactions` | List transactions (with filters) | 200 |
| PUT | `/api/transactions/{id}` | Update a transaction | 200 |
| DELETE | `/api/transactions/{id}` | Delete a transaction | 200 |

**Query Parameters for GET:** `startDate`, `endDate`, `categoryId` (all optional)

```json
POST /api/transactions
{
  "amount": 50000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary"
}
// Response: { "id": 1, "amount": 50000.00, "date": "2024-01-15",
//             "category": "Salary", "description": "January Salary", "type": "INCOME" }
```

### 3. Categories

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/categories` | List all categories | 200 |
| POST | `/api/categories` | Create custom category | 201 |
| DELETE | `/api/categories/{name}` | Delete custom category | 200 |

**Default Categories:** Salary (INCOME), Food, Rent, Transportation, Entertainment, Healthcare, Utilities (EXPENSE)

### 4. Savings Goals

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/goals` | Create a savings goal | 201 |
| GET | `/api/goals` | List all goals with progress | 200 |
| GET | `/api/goals/{id}` | Get specific goal | 200 |
| PUT | `/api/goals/{id}` | Update goal | 200 |
| DELETE | `/api/goals/{id}` | Delete goal | 200 |

**Progress Calculation:** `currentProgress = Total Income - Total Expenses` since `startDate`

### 5. Reports

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/reports/monthly/{year}/{month}` | Monthly financial report | 200 |
| GET | `/api/reports/yearly/{year}` | Yearly financial report | 200 |

```json
GET /api/reports/monthly/2024/1
{
  "month": 1, "year": 2024,
  "totalIncome": { "Salary": 50000.00 },
  "totalExpenses": { "Food": 5000.00, "Rent": 15000.00 },
  "netSavings": 30000.00
}
```

## Error Handling

| Status | Description |
|--------|-------------|
| 400 | Bad Request — validation errors, malformed input |
| 401 | Unauthorized — invalid credentials, expired session |
| 403 | Forbidden — accessing another user's data |
| 404 | Not Found — resource doesn't exist |
| 409 | Conflict — duplicate category names |

## Project Structure

```
src/main/java/com/aaryav/finance/
├── FinanceManagerApplication.java
├── config/
│   ├── SecurityConfig.java          # Session-based security
│   ├── CustomUserDetailsService.java
│   └── DataInitializer.java         # Seeds default categories
├── controller/
│   ├── AuthController.java
│   ├── TransactionController.java
│   ├── CategoryController.java
│   ├── GoalController.java
│   └── ReportController.java
├── service/
│   ├── AuthService.java
│   ├── TransactionService.java
│   ├── CategoryService.java
│   ├── GoalService.java
│   └── ReportService.java
├── repository/
│   ├── UserRepository.java
│   ├── TransactionRepository.java
│   ├── CategoryRepository.java
│   └── GoalRepository.java
├── entity/
│   ├── User.java
│   ├── Transaction.java
│   ├── Category.java
│   ├── CategoryType.java
│   └── Goal.java
├── dto/
│   ├── request/
│   └── response/
└── exception/
    ├── GlobalExceptionHandler.java
    └── ...custom exceptions
```

## Author

**Aaryav Chaudhary**
- GitHub: [@Aaryav1130](https://github.com/Aaryav1130)
- Email: aaryav1130@gmail.com
