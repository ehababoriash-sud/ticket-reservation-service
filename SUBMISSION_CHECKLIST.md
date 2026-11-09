# Before submitting

- [ ] Install Docker and run `docker compose up --build`.
- [ ] Confirm `/health` returns HTTP 200 and `{"status":"UP"}`.
- [ ] Run `mvn test` with Docker running; resolve any environmental issues.
- [ ] Exercise create event, hold, confirm, cancel, and list bookings.
- [ ] Explain event row locking, the DB CHECK constraint, expiry and idempotency without notes.
- [ ] Review the source yourself and make small, truthful Git commits as you work.
- [ ] Push to your own GitHub repository and submit its URL.

**Verification status:** Generated in an environment without Maven or Docker; compilation, tests and container startup have NOT been executed here. Do not claim otherwise.
