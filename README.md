# Ticket Reservation Service

A backend ticket reservation service built with **Java 21, Spring Boot 3, PostgreSQL, and Docker**.

The system supports event creation, temporary seat reservations (holds), booking confirmation, cancellation, and automatic expiration of holds.

It is designed to prevent **overselling**, including when multiple customers attempt to reserve tickets concurrently.

Inventory is managed by quantity rather than individual assigned seat numbers.

## Tech Stack

- Java 21

- Spring Boot 3

- Spring Data JPA / Hibernate

- PostgreSQL

- Flyway database migrations

- Maven

- Docker and Docker Compose

- JUnit 5

- Testcontainers

## Features

- Create events with a fixed ticket capacity.

- View available, held, and booked tickets.

- Create temporary holds lasting 10 minutes by default.

- Confirm holds into bookings.

- Cancel active holds.

- Automatically expire outdated holds.

- Limit customers to 6 held tickets per event.

- Support idempotent booking confirmation.

- Prevent overselling during concurrent requests.

- Maintain inventory consistency across multiple application instances.

## Running the Application

### Requirements

- Docker Desktop

- Docker Compose

Java and Maven are not required locally when running the application through Docker Compose.

### Start the Application

Open a terminal in the project root and run:

```bash

docker compose up --build

```

The command builds and starts the Spring Boot application and PostgreSQL database.

### Service URLs

- API: http://localhost:8080

- Health endpoint: http://localhost:8080/health

- PostgreSQL: localhost:5432 (for local inspection)

### Health Check

```bash

curl -i http://localhost:8080/health

```

Expected response:

```json

{

&#x20; "status": "UP"

}

```

### Stop the Application

```bash

docker compose down

```

## Configuration

The application supports the following environment variables:

| Variable | Description | Default |

|---|---|---|

| PORT | API server port | 8080 |

| DB\_PORT | PostgreSQL port | 5432 |

| DB\_URL | Database connection URL | Configured by Compose |

| DB\_USER | Database username | Configured by Compose |

| DB\_PASSWORD | Database password | Configured by Compose |

| HOLD\_DURATION | Hold duration (ISO-8601) | PT10M |

| EXPIRY\_SWEEP\_MS | Expiration scan interval | 30000 |

Docker Compose provides the database configuration to the application container.

Production deployments should use secure secret management rather than example credentials.

## REST API

### 1. Create Event

**POST /events**

```bash

curl -s -X POST http://localhost:8080/events \\

&#x20; -H 'Content-Type: application/json' \\

&#x20; -d '{"name":"Summer Concert","startsAt":"2030-07-01T19:00:00Z","totalSeats":10}'

```

Creates an event with a fixed number of available tickets.

### 2. Get Event Inventory

**GET /events/{eventId}**

```bash

curl -s http://localhost:8080/events/$EVENT\_ID

```

Returns the total, available, held, and booked ticket counts.

The following invariant must always hold:

```text

available + held + booked = totalSeats

```

### 3. Create Hold

**POST /events/{eventId}/holds**

```bash

curl -s -X POST http://localhost:8080/events/$EVENT\_ID/holds \\

&#x20; -H 'Content-Type: application/json' \\

&#x20; -d '{"customerId":"11111111-1111-1111-1111-111111111111","quantity":2}'

```

Creates a temporary reservation for the requested number of tickets.

Holds expire after 10 minutes by default.

### 4. Confirm Booking

**POST /holds/{holdId}/confirm**

```bash

curl -s -X POST http://localhost:8080/holds/$HOLD\_ID/confirm

```

Converts an active hold into a confirmed booking.

Repeated confirmation of an already confirmed hold returns the existing booking without creating a duplicate.

### 5. Cancel Hold

**DELETE /holds/{holdId}**

```bash

curl -i -X DELETE http://localhost:8080/holds/$HOLD\_ID

```

Cancels an active hold and releases its reserved tickets.

### 6. Get Customer Bookings

**GET /customers/{customerId}/bookings**

```bash

curl -s http://localhost:8080/customers/11111111-1111-1111-1111-111111111111/bookings

```

Returns the customer's confirmed bookings.

## HTTP Response Codes

| Status | Meaning |

|---|---|

| 200 | Successful request or repeated booking confirmation |

| 201 | Event, hold, or booking created |

| 204 | Hold cancelled successfully |

| 400 | Invalid request |

| 404 | Event or hold not found |

| 409 | Capacity, customer limit, or state conflict |

| 410 | Hold expired |

Repeated cancellation of an already cancelled or expired hold returns HTTP 204.

## Concurrency and Overselling Prevention

Preventing overselling is a core requirement of the service.

The application uses PostgreSQL row-level locking within database transactions.

Inventory-changing operations acquire a lock on the relevant event row using:

```sql

SELECT ... FOR UPDATE

```

This ensures that concurrent reservation operations affecting the same event are serialized at the database level.

The approach works across multiple application instances sharing the same PostgreSQL database.

### Inventory Consistency

The following invariant is maintained:

```text

available + held + booked = totalSeats

```

The system does not permit more active reservations and bookings than the event capacity.

### Expired Holds

Expired holds are released using:

1\. A scheduled background expiration process.

2\. Lazy expiration during relevant event operations.

An expired hold cannot be confirmed, even if the scheduled expiration process has not yet executed.

For additional architectural details, see \[DESIGN.md](DESIGN.md).

## Automated Testing

The project includes unit, validation, and integration tests.

### Running Tests Locally

Requirements:

- Java 21

- Maven 3.9+

- Running Docker daemon

```bash

mvn test

```

Integration tests use Testcontainers with a real PostgreSQL database rather than an in-memory H2 database.

The test suite covers:

- API input validation

- Inventory lifecycle

- Customer hold limits

- Hold expiration

- Concurrent reservation attempts

- Idempotent booking confirmation

- Database-backed inventory consistency

### Running Tests with Docker on Windows

If Java or Maven are not installed locally, tests can run inside a Maven Docker container.

Requirements:

- Docker Desktop running

- Windows PowerShell

- Docker Engine accessible to containers

Run the following command from the project root:

```powershell

docker run --rm `

&#x20; -v "${PWD}:/app" `

&#x20; -v /var/run/docker.sock:/var/run/docker.sock `

&#x20; -e DOCKER\_HOST=unix:///var/run/docker.sock `

&#x20; -e DOCKER\_API\_VERSION=1.44 `

&#x20; -e JAVA\_TOOL\_OPTIONS="-Dapi.version=1.44" `

&#x20; -e TESTCONTAINERS\_HOST\_OVERRIDE=host.docker.internal `

&#x20; -w /app `

&#x20; maven:3.9-eclipse-temurin-21 `

&#x20; mvn test

```

The Java Docker API version override was required in the tested environment due to a compatibility issue involving Docker Desktop 4.99.0 and Testcontainers 1.20.6.

This is an environment-specific workaround and may not be necessary with other Docker versions.

### Automated Test Results

The test suite was executed successfully in the local development environment.

```text

Tests run: 12

Failures: 0

Errors: 0

Skipped: 0

BUILD SUCCESS

```

All 12 automated tests passed.

## Manual Concurrency Test

A separate manual concurrency test was performed against the running application.

### Test Scenario

- Event capacity: 10 tickets

- Concurrent workload: 200 hold requests

- Requested quantity: 1 ticket per request

- Distinct customer IDs

### Results

| Metric | Result |

|---|---:|

| Total requests | 200 |

| Successful holds | 10 |

| Rejected requests | 190 |

| Event capacity | 10 |

| Available tickets | 0 |

| Held tickets | 10 |

| Booked tickets | 0 |

### Final Inventory

```text

totalSeats = 10

available = 0

held = 10

booked = 0

```

Inventory verification:

```text

0 + 10 + 0 = 10

```

**Result: No overselling was observed during the manual concurrency test.**

These results describe local verification and do not replace production-scale performance testing.

## Operational Notes

- Inventory updates are protected by PostgreSQL row-level locks.

- Each event can become a contention hotspot under heavy load.

- Expired holds are handled through scheduled and lazy expiration.

- Structured JSON logs are written to standard output.

- The health endpoint includes database health checks.

- Flyway manages database schema migrations.

## Scope and Limitations

The service focuses on the core ticket reservation lifecycle and inventory correctness.

The following features are outside the scope of this exercise:

- User authentication and authorization

- Payment processing

- Assigned seat maps

- Distributed tracing

- Production monitoring infrastructure

For design decisions, trade-offs, and potential scaling improvements, see \[DESIGN.md](DESIGN.md).

## Project Structure

```text

ticket-reservation-service/

├── src/

│   ├── main/

│   │   ├── java/com/example/tickets/

│   │   │   ├── api/

│   │   │   ├── config/

│   │   │   ├── domain/

│   │   │   ├── repo/

│   │   │   └── service/

│   │   └── resources/

│   │       └── db/migration/

│   └── test/

│       └── java/com/example/tickets/

├── Dockerfile

├── docker-compose.yml

├── pom.xml

├── README.md

└── DESIGN.md

```

## Summary

This project demonstrates a transactional ticket reservation backend using Java, Spring Boot, and PostgreSQL.

Its primary focus is maintaining inventory correctness, handling temporary reservations, and preventing overselling under concurrent requests.

The application was successfully started using Docker Compose, all 12 automated tests passed, and a separate 200-request manual concurrency test completed without observed overselling.
