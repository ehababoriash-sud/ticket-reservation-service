# Ticket Reservation Service

Java 21 / Spring Boot 3 / PostgreSQL ticket inventory service. **Inventory is by quantity, not assigned seat numbers** (the supplied API requests N seats, not seat IDs).

## Run

Requirements: Docker with Compose. From this directory:

```bash
docker compose up --build
```

- API: `http://localhost:8080`
- Health: `curl -i http://localhost:8080/health` (checks DB through Actuator)
- PostgreSQL: localhost:5432 (for local inspection only)

Configuration: `PORT` (default 8080), `DB_PORT` (5432), `DB_URL`, `DB_USER`, `DB_PASSWORD`, `HOLD_DURATION` (ISO-8601 duration, default `PT10M`), `EXPIRY_SWEEP_MS` (default 30000). Compose passes database settings to the API container. For production use secrets rather than example credentials.

## API examples

```bash
# Create an event (choose a future date)
curl -s -X POST http://localhost:8080/events -H 'Content-Type: application/json' \
  -d '{"name":"Summer Concert","startsAt":"2030-07-01T19:00:00Z","totalSeats":10}'
# Copy returned id to EVENT_ID
curl -s http://localhost:8080/events/$EVENT_ID

# Create a hold (customer ID is a UUID)
curl -s -X POST http://localhost:8080/events/$EVENT_ID/holds -H 'Content-Type: application/json' \
  -d '{"customerId":"11111111-1111-1111-1111-111111111111","quantity":2}'
# Copy returned id to HOLD_ID
curl -s -X POST http://localhost:8080/holds/$HOLD_ID/confirm
curl -i -X DELETE http://localhost:8080/holds/$HOLD_ID
curl -s http://localhost:8080/customers/11111111-1111-1111-1111-111111111111/bookings
```

The create endpoints return `201`; invalid input `400`; unknown event/hold `404`; capacity/limit/state conflicts `409`; expired confirmation `410`; cancel succeeds with `204`. Confirming an already confirmed hold returns the same booking (200). Repeated cancel on a cancelled/expired hold returns 204.

## Tests

Requires Java 21, Maven 3.9+ and a running Docker daemon (Testcontainers starts a real PostgreSQL):

```bash
mvn test
```

Tests include validation, inventory lifecycle, customer cap, expiry, 200 competing holds for 10 seats, and simultaneous confirmation retries. Integration tests exercise PostgreSQL rather than H2. To simulate two API processes manually, start a second service pointing at the same DB with a different HTTP port; all event locks are database-scoped.

## Operational notes

- All mutations of inventory take a PostgreSQL row lock on the event (`SELECT ... FOR UPDATE`) inside a transaction.
- Each event is a contention hotspot by design; see `DESIGN.md` for alternatives and scaling.
- A scheduled sweeper plus lazy expiration during event operations releases expired holds. A stale hold is never confirmable even before the sweeper runs.
- JSON logs go to stdout; `/health` checks Actuator health including the database.
- No authentication, payment integration, seat-map assignment, or distributed tracing is included; these are outside this exercise's core scope.

## Suggested commit sequence

The provided ZIP is a snapshot and does not claim to contain a historical Git commit log. When building your own repository, commit changes as you implement and validate each step: `chore: bootstrap`, `feat: schema`, `feat: events`, `feat: holds`, `feat: booking`, `feat: expiry`, `test: concurrency`, `docs: architecture`.
