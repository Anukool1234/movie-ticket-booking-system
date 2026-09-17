# Movie Ticket Booking System

Simple Spring Boot REST API for booking movie tickets across cities and theaters, with seat holds, pricing tiers, discounts, payments, refunds, and role-based access.

Built to be easy to read, run, and explain — not over-engineered.

---

## Tech stack

| Choice | Why |
|--------|-----|
| Java 17 + Spring Boot 3.3 | Required stack; mature ecosystem |
| Spring Data JPA + H2 | Simple persistence; no DB install needed |
| Spring Security + HTTP Basic | Simple RBAC without token-management code |
| `@Scheduled` | Auto-release expired seat holds |
| `@Async` | Confirmation / reminder notifications without blocking APIs |
| Optimistic locking (`@Version`) | Prevent double-booking the same seat |

---

## Features

- **Catalog**: cities → theaters → seat layouts → shows
- **Seat booking**: time-bound holds (default 10 minutes), then pay to confirm
- **Pricing**: regular / premium seats + weekend multiplier + discount codes
- **Payment**: simulated success/failure (no real gateway)
- **Cancellation + refund**: based on configurable hours-before-show policies
- **Concurrency**: optimistic lock on `ShowSeat` so two users cannot book the same seat
- **Notifications**: async confirmation, cancellation, and pre-show reminders (logged + stored)
- **RBAC**: admin manages catalog/policies; customer books/cancels/views history

---

## Assumptions (documented)

1. **Single monolith / single DB** — no microservices (out of scope).
2. **H2 in-memory** — data resets on restart; swap URL for Postgres if needed.
3. **Payment is simulated** — `success: true/false` in the pay API.
4. **Notifications are simulated** — written to DB + application logs (no email/SMS provider).
5. **One screen per theater** — seat layout belongs directly to the theater (keeps model simple).
6. **Weekend pricing** — admin marks `weekendShow=true` on a show; `DEFAULT` pricing tier supplies the multiplier (default 1.20).
7. **Hold duration** — configurable via `app.seat-hold.duration-minutes` (default 10).
8. **Refund rules** — highest matching `minHoursBeforeShow` wins (seeded: 100% ≥24h, 50% ≥6h, 0% otherwise).
9. **Users are seeded for the demo** — registration and token APIs are intentionally omitted.
10. **HTTP Basic authentication** — sufficient for demonstrating RBAC; HTTPS would be required in production.

---

## How to run

```bash
# Java 17+ and Maven required
mvn spring-boot:run
```

App: `http://localhost:8080`  
H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:movieticket`)

### Demo users (seeded)

| Email | Password | Role |
|-------|----------|------|
| `admin@movie.com` | `admin123` | ADMIN |
| `user@movie.com` | `user123` | CUSTOMER |

Seeded data also includes Mumbai/Delhi theaters, shows (*Inception*, *Interstellar*), discount `SAVE10` (10% off), and refund policies.

---

## Quick API walkthrough

Public catalog endpoints need no credentials. Protected endpoints use HTTP Basic through
curl's `-u email:password` option, so there is no separate login or token step.

### 1. Browse shows & seats
```bash
curl -s http://localhost:8080/api/shows
curl -s http://localhost:8080/api/shows/1/seats
```

### 2. Hold seats (create booking)
```bash
curl -s -X POST http://localhost:8080/api/bookings \
  -u user@movie.com:user123 \
  -H 'Content-Type: application/json' \
  -d '{"showId":1,"showSeatIds":[1,2],"discountCode":"SAVE10"}'
```

### 3. Pay
```bash
curl -s -X POST http://localhost:8080/api/bookings/pay \
  -u user@movie.com:user123 \
  -H 'Content-Type: application/json' \
  -d '{"bookingId":1,"success":true}'
```

### 4. History / cancel
```bash
curl -s -u user@movie.com:user123 http://localhost:8080/api/bookings
curl -s -u user@movie.com:user123 -X POST http://localhost:8080/api/bookings/1/cancel
```

### Admin example (create city)
```bash
curl -s -X POST http://localhost:8080/api/admin/cities \
  -u admin@movie.com:admin123 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Pune"}'
```

---

## Main API map

| Method | Path | Access | Purpose |
|--------|------|--------|---------|
| GET | `/api/cities`, `/api/theaters`, `/api/shows` | Public | Browse catalog |
| GET | `/api/shows/{id}/seats` | Public | Seat map + prices |
| POST | `/api/bookings` | Customer | Hold seats |
| POST | `/api/bookings/pay` | Customer | Confirm payment |
| POST | `/api/bookings/{id}/cancel` | Customer | Cancel + refund |
| GET | `/api/bookings` | Customer | Booking history |
| POST | `/api/admin/**` | Admin | Cities, theaters, seats, shows, discounts, pricing, refunds |

---

## Project structure

```
com.movieticket
├── controller   # REST endpoints
├── service      # Business logic
├── entity       # JPA models
├── repository   # DB access
├── dto          # Request/response objects
├── security     # HTTP Basic authentication + RBAC
├── exception    # Global error handling
├── scheduler    # Hold expiry + reminders
└── config       # Demo-data seeder
```

---

## How double-booking is prevented

`ShowSeat` has a `@Version` field. When two users try to hold the same seat:

1. Both read `AVAILABLE`
2. First transaction saves `HELD` and increments version
3. Second save fails with optimistic lock → API returns a clear business error

Expired holds are released by a scheduler every minute (and treated as available when browsing).

---

## Tests

```bash
mvn test
```

- `PricingServiceTest` — unit tests for seat price, weekend multiplier, discounts  
- `BookingFlowIntegrationTest` — full hold → pay → history → cancel, RBAC, double-book prevention  

---

## Configuration knobs

See `src/main/resources/application.yml`:

- `app.seat-hold.duration-minutes`
- `app.notification.reminder-minutes-before`
