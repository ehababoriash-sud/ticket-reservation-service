CREATE TABLE events (
 id UUID PRIMARY KEY, name VARCHAR(200) NOT NULL, starts_at TIMESTAMPTZ NOT NULL,
 total_seats INTEGER NOT NULL CHECK (total_seats > 0),
 available_seats INTEGER NOT NULL CHECK (available_seats >= 0),
 held_seats INTEGER NOT NULL DEFAULT 0 CHECK (held_seats >= 0),
 booked_seats INTEGER NOT NULL DEFAULT 0 CHECK (booked_seats >= 0),
 CONSTRAINT seat_balance CHECK (available_seats + held_seats + booked_seats = total_seats)
);
CREATE TABLE holds (
 id UUID PRIMARY KEY, event_id UUID NOT NULL REFERENCES events(id),
 customer_id UUID NOT NULL, quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 6),
 status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','CONFIRMED','CANCELLED','EXPIRED')),
 expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_holds_event_customer ON holds(event_id, customer_id, status);
CREATE INDEX idx_holds_expiry ON holds(expires_at) WHERE status = 'ACTIVE';
CREATE TABLE bookings (
 id UUID PRIMARY KEY, hold_id UUID NOT NULL UNIQUE REFERENCES holds(id),
 event_id UUID NOT NULL REFERENCES events(id), customer_id UUID NOT NULL,
 quantity INTEGER NOT NULL CHECK (quantity > 0), confirmed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_bookings_customer ON bookings(customer_id);
