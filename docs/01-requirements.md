# 1. Requirements

> Every class in this project traces back to a line in this file. If a class can't name the requirement it serves, it shouldn't exist yet.

## 1.1 Problem statement

A customer picks a vehicle at a branch for a date range, pays, picks it up, uses it and returns it. Admins run branches and their fleet. The system must never rent the same vehicle to two people for overlapping dates.

## 1.2 Actors

| Actor | Description |
|---|---|
| **Customer** | Registered user with a driving licence. Searches, books, pays, cancels, views history. |
| **Admin** | Branch staff. Manages branches and vehicles, hands vehicles over (pickup) and receives them (return). |
| **System (scheduler)** | Background job that expires unpaid bookings. |
| **Payment gateway** | External system (mocked) that charges and refunds Card / UPI / Cash. |

## 1.3 Functional requirements

| ID | Requirement | Where it lives |
|---|---|---|
| FR-01 | A user can **register** as Customer or Admin. Customers must give a driving-licence number. Emails are unique. | `UserServiceImpl.register` |
| FR-02 | A user can **log in** with email and password. Passwords are stored hashed. | `UserServiceImpl.login`, `PasswordHasher` |
| FR-03 | An admin can **create branches** (name, address, city). | `BranchServiceImpl.createBranch` |
| FR-04 | An admin can **add vehicles** (cars and bikes) to a branch. Registration numbers are unique. | `VehicleServiceImpl.addCar / addBike` |
| FR-05 | An admin can **change a vehicle's status** (Available, Under maintenance, Inactive). | `VehicleServiceImpl.changeStatus` |
| FR-06 | A customer can **search available vehicles** by branch and date range, optionally filtering by category, car type and minimum seats. Results are sorted by price. | `VehicleServiceImpl.searchAvailable` |
| FR-07 | A customer can **create a booking** for a vehicle and date range. The booking starts as *Pending payment*. | `BookingServiceImpl.createBooking` |
| FR-08 | The system **calculates the price** as daily rate × number of days (both dates inclusive). The rule is pluggable. | `PricingStrategy` |
| FR-09 | A customer can **pay** for their booking by Card, UPI or Cash. Success confirms the booking; failure leaves it pending so they can retry. | `BookingServiceImpl.payForBooking`, `PaymentServiceImpl` |
| FR-10 | A customer can **cancel** their booking before pickup. If it was paid, the payment is **refunded**. | `BookingServiceImpl.cancelBooking` |
| FR-11 | An admin can mark **pickup** of a confirmed booking, on or after its start date. | `BookingServiceImpl.markPickup` |
| FR-12 | An admin can mark **return**. If the vehicle comes back after the end date, a **late fee** is charged. | `BookingServiceImpl.markReturn`, `LateFeePolicy` |
| FR-13 | A customer can view their **booking history** and a booking's **payments**. | `BookingServiceImpl.getUserBookings / getPayments` |
| FR-14 | Unpaid bookings **expire** automatically after 15 minutes, releasing the vehicle. | `PendingBookingCleanupJob` |
| FR-15 | Interested parties are **notified** of booking events (created, confirmed, picked up, returned, cancelled, expired). | `BookingEventListener` |
| FR-16 | All operations are available as a **REST API** documented with **OpenAPI 3.1**, testable from Swagger UI, Postman, Requestly or curl. | `HttpApiServer`, `openapi.yaml`, [10-api-testing-guide.md](10-api-testing-guide.md) |

### Business rules

| ID | Rule |
|---|---|
| BR-01 | Start date cannot be in the past; end date cannot be before start date. |
| BR-02 | A rental is at most **30 days**; bookings open at most **90 days** in advance. |
| BR-03 | Two bookings for the same vehicle overlap when `existing.start <= new.end AND existing.end >= new.start`. Only *Pending payment*, *Confirmed* and *Ongoing* bookings block a vehicle. |
| BR-04 | A vehicle that is not *Available* cannot be searched, booked or handed over. |
| BR-05 | Late fee = late days × daily rate × **1.5**. Returning any time on the end date is on time. |
| BR-06 | UPI payments above **₹1,00,000** per transaction are declined (mock gateway rule). |
| BR-07 | Only the booking's owner (or an admin) can cancel it; only the owner can pay for it. |
| BR-08 | Booking status changes follow the state machine in [06-flows-and-state-machine.md](06-flows-and-state-machine.md). Any other change is rejected. |

## 1.4 Non-functional requirements

| ID | Quality | Requirement | How it is met |
|---|---|---|---|
| NFR-01 | **Consistency** | No double booking, even when many users book the same vehicle at the same instant. | Per-vehicle lock around check-and-save (`VehicleLockManager`). Proven by a 32-thread test. |
| NFR-02 | **Correctness of money** | No floating-point rounding errors. | `BigDecimal` with scale 2 and `HALF_UP` everywhere. |
| NFR-03 | **Auditability** | Every payment attempt, including failures, is stored. | `PaymentServiceImpl.charge` saves FAILED payments too. |
| NFR-04 | **Security** | Passwords never stored or returned in plain text; role-based access for admin actions; same error for wrong email and wrong password. | `PasswordHasher` (salted SHA-256), `requireAdmin`, DTOs without hashes. |
| NFR-05 | **Extensibility** | New pricing rules, payment methods, vehicle types and notification channels can be added without editing existing classes. | Strategy, Factory, abstract `Vehicle`, Observer (Open/Closed principle). |
| NFR-06 | **Maintainability** | Clear layers, one responsibility per class, dependencies point one way. | Controller → Service → Repository; constructor injection. |
| NFR-07 | **Testability** | Business logic can be tested without real time or a real database. | Injected `Clock`, in-memory repositories, fresh system per test. |
| NFR-08 | **Thread safety** | Shared state is safe under concurrent access. | `ConcurrentHashMap`, `AtomicLong`, `CopyOnWriteArrayList`, locks. |
| NFR-09 | **Availability of inventory** | Abandoned unpaid bookings do not hold vehicles forever. | 15-minute payment window + cleanup job. |
| NFR-10 | **Portability** | Builds and runs with only a JDK 17+, no external libraries. | Zero dependencies, `run.bat` / `run.sh`. |
| NFR-11 | **Observability & audit** | Every request is traceable end to end; every business action (including failed and denied attempts) is audited; no secrets in logs. | Trace id per request (`TraceContext`), levelled logs, `AuditLogger` → `logs/audit-0.log`, `LogSanitizer`. See [09](09-logging-and-audit.md). |

## 1.5 Out of scope (v1)

Chosen on purpose, so the core design stays visible. See [08-design-decisions-and-roadmap.md](08-design-decisions-and-roadmap.md) for how each would be added.

- Real payment gateway (mocked), JWT/session auth (login just returns the user)
- Persistent database (in-memory repositories)
- One-way rentals (pickup at A, drop at B), hourly rentals
- Coupons, insurance add-ons, security deposit, damage inspection
- Partial refunds / cancellation fees, reviews and ratings
