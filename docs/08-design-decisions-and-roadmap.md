# 8. Design Decisions, Thinking Process and Roadmap

## 8.1 The design procedure used (repeat it for any LLD problem)

1. **Say the business in one sentence.** "A customer picks a vehicle at a branch for dates, pays, picks it up, returns it."
2. **List functional requirements** (what it does) and keep v1 small. Write down what is *out of scope*.
3. **List non-functional requirements** (qualities). Find the one that hurts most: here, *no double booking*.
4. **Nouns → entities:** User, Branch, Vehicle, Booking, Payment.
5. **Verbs → service methods:** search, book, pay, cancel, pickup, return.
6. **Fixed categories and lifecycles → enums:** role, vehicle type, booking status.
7. **Relationships:** for each pair ask "how many of X does one Y have?" Put the FK on the many side.
8. **Find the central transaction entity** (Booking) and its state machine.
9. **Trace every flow end to end** (controller → service → repository and back). Missing fields and methods show up here.
10. **Find what will change** and hide it behind an interface (pricing, payments, notifications, storage).
11. **Find the hard parts** (concurrency, money, time) and solve them explicitly.
12. **Build in layers:** model → repository → service → controller, then tests.

## 8.2 Key decisions and trade-offs

| # | Decision | Why | Alternative considered |
|---|---|---|---|
| D1 | No `BOOKED` in `VehicleStatus`; availability computed from bookings | Availability depends on dates; a flag would be wrong for every date but one | Status flag + date table (duplicated truth) |
| D2 | `Booking` enforces its own transitions (rich model) | Impossible to bypass the state machine from any service | Status setter + checks scattered in services |
| D3 | `BookingService` orchestrates payments; `PaymentService` is booking-agnostic | No circular dependency; payments reusable for other products | Payment service confirming bookings |
| D4 | Booking → Payment is 1 : N with `PaymentPurpose` | Late fees and failed attempts are separate money movements, all audited | One payment per booking with mutable amount |
| D5 | Per-vehicle lock around check-and-save | Correct under concurrency; other vehicles not blocked | One global lock (slow), no lock (double bookings) |
| D6 | Inclusive dates, late if returned on a later calendar day | Matches how customers read "10–12 Nov" | Hour-based billing (planned for v2) |
| D7 | Unpaid bookings hold the vehicle for 15 minutes | Prevents someone paying for a car another customer already holds, without blocking inventory forever | No hold (race at payment time) |
| D8 | Entities store foreign-key ids, not object references | Mirrors tables, keeps in-memory model simple | Object graph (would need lazy loading) |
| D9 | Injected `Clock` everywhere | Time-dependent rules become testable without sleeping | `LocalDate.now()` (untestable) |
| D10 | Zero external libraries, own mini test runner | Builds anywhere with a JDK; nothing hides the design | JUnit/Lombok/Spring (planned in the Spring port) |
| D11 | Role checks inside services, not controllers | No future endpoint can forget the check | Checks in controllers only |
| D12 | Single `User` + `UserRole` enum | Customer and Admin differ only in permissions | `abstract Person → Customer, Admin` (if fields diverge) |

## 8.3 Known limitations (honest list)

- In-memory storage: data is lost on restart.
- JVM-level locking: correct for one instance only.
- Changing a vehicle to *Under maintenance* does not notify customers with future bookings; pickup is simply refused.
- Full refund on any cancellation; no cancellation fee.
- Login returns the user; there is no session or token.
- Payment gateways are mocks; no idempotency keys or webhooks.

## 8.4 Roadmap

| Version | Feature | Design impact |
|---|---|---|
| v1.1 | Cancellation policy (free until 24 h before, then 50%) | New `CancellationPolicy` strategy |
| v1.1 | Hourly rentals | `LocalDateTime` ranges, `HourlyPricingStrategy` |
| v1.2 | Coupons and seasonal pricing | Decorator around `PricingStrategy` |
| v1.2 | Security deposit and damage inspection | `Inspection` entity, `PaymentPurpose.DEPOSIT` |
| v1.3 | One-way rentals | `dropBranchId` on Booking; vehicle's branch changes on return |
| v2.0 | **Spring Boot port**: JPA + PostgreSQL, Spring Security + JWT, Flyway, JUnit 5 + Testcontainers, OpenAPI docs | See mapping table in [02-architecture.md](02-architecture.md#25-how-this-maps-to-spring-boot) |
| v2.1 | Database-level double-booking guard (exclusion constraint or `FOR UPDATE`) | Replace `VehicleLockManager` |
| v2.2 | Notifications via Kafka; email/SMS service | New `BookingEventListener` publishing to a topic |
| v2.3 | Docker image, docker-compose (app + DB), GitHub Actions CI | DevOps layer |
