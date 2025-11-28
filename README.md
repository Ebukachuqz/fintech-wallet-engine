# Wallet Engine

A production-grade, concurrency-safe Wallet Engine built with Spring Boot and PostgreSQL. This system handles funds, enforces ACID compliance, prevents double-spending via pessimistic locking, and ensures idempotency for all write operations.

## Overview

This application serves as a backend engine for a digital wallet system. It is designed to be robust against race conditions (e.g., two concurrent debit requests) and network failures (via idempotency keys).

## Key Features

* **Create Wallet:** Initialize a wallet for a user (via Email).

* **Fund Transfer:** Credit and Debit capabilities.

* **Concurrency Safety:** Uses Database **Pessimistic Locking** to prevent race conditions during balance updates.

* **Idempotency:** Prevents duplicate processing of the same request using `Idempotency-Key` headers.

* **Audit Trail:** Immutable transaction history with auto-generated references.

* **Status Management:** Activate/Deactivate wallets to freeze funds.

## Tech Stack

* **Language:** Java 25

* **Framework:** Spring Boot 4.0.0

* **Database:** PostgreSQL 15

* **Persistence:** Spring Data JPA / Hibernate

* **Containerization:** Docker & Docker Compose

* **Testing:** JUnit 5, Mockito

## Architecture & Design Decisions

### 1. Handling Concurrency (The "Double Spend" Problem)

To ensure that two simultaneous debit requests do not put the wallet into a negative state, I implemented **Pessimistic Locking** at the database level.

* **Mechanism:** When a transaction begins, the specific Wallet row is locked using `LockModeType.PESSIMISTIC_WRITE`.

* **Result:** Other transactions targeting this wallet must wait until the lock is released, ensuring serial execution of balance updates.

### 2. Idempotency

To handle network retries safely, all write operations (Credit/Debit) require an `Idempotency-Key` header.

* The system checks the `idempotency_keys` table before processing.

* If the key exists, the request is rejected immediately (or can be configured to return the cached response).

### 3. Rich Domain Model

Business logic regarding "Insufficient Funds" or "Inactive Wallet" is encapsulated within the `Wallet` entity itself, preventing anemic domain models and ensuring a wallet can never be in an invalid state.

## Configuration & Secrets

The application handles secrets using environment variables. You can set these in your system environment, or create a `.env` file in the project root.

### Using a `.env` file (Recommended for Local Dev)

1. Copy the example file: `cp .env.example .env`

2. Edit the `.env` file with your actual secrets.

3. **Note:** The `.env` file is git-ignored to protect your secrets.

| **Variable** | **Description** | **Default (Local)** |
| `DATABASE_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://localhost:5432/wallet_engine` |
| `DB_USERNAME` | Database Username | `wallet_user` |
| `DB_PASSWORD` | Database Password | `wallet_password` |

**Note:** If running locally with the provided Docker Compose file for the database, the defaults above will work automatically.

## Running the Application

You can run the application in two ways: fully containerized (recommended for consistency) or in hybrid mode (recommended for active development).

### Option 1: Run Fully in Docker (App + DB)

This starts both the PostgreSQL database and the Spring Boot application in isolated containers.

1. **Build and Start:**

   ```bash
   docker-compose up --build
   ```

2. **Access:**
   The application will be available at `http://localhost:8080`.

### Option 2: Hybrid Mode (DB in Docker, App Local)

This is useful if you want to debug the Java code in your IDE while keeping the database in Docker.

**Step 1: Start the Database**

Use Docker Compose to spin up only the PostgreSQL container.

```bash
docker-compose up -d wallet-db
```

This will start PostgreSQL on port `5432`.

**Step 2: Run the Application**

You can run the Spring Boot application using the Maven wrapper included in the project. The application will automatically pick up variables from your `.env` file.

**Using Terminal:**

```bash
./mvnw spring-boot:run
```

**Using IDE (IntelliJ/Eclipse):**

1. Open the project.

2. Navigate to `src/main/java/com/example/fintech_wallet_engine/WalletEngineApplication.java`.

3. Right-click and select **Run**.

The application will start on port `8080`.

## API Documentation

**Base URL:** `http://localhost:8080/api/v1/wallets`

### 1. Create Wallet

**POST** `/create`

Body:

```json
{
    "email": "user@example.com"
}
```

### 2. Get Wallet by ID

**GET** `/id/{uuid}`

Example: `/id/550e8400-e29b-41d4-a716-446655440000`

### 3. Get Wallet by Email

**GET** `/email/{email}`

Example: `/email/user@example.com`

### 4. Credit Wallet

**POST** `/credit`

Header: `Idempotency-Key: unique-key-1`

Body:

```json
{
    "email": "user@example.com",
    "amount": 5000,
    "description": "Salary Deposit"
}
```

### 5. Debit Wallet

**POST** `/debit`

Header: `Idempotency-Key: unique-key-2`

Body:

```json
{
    "email": "user@example.com",
    "amount": 1500,
    "description": "Purchase"
}
```

### 6. Deactivate/Activate Wallet

**PATCH** `/{id}/status`

Body:

```json
{
    "status": "INACTIVE"
}
```

## Testing

To run the unit tests (which cover locking logic, insufficient funds logic, and idempotency checks):

```bash
mvn test
```