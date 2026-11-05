# 6. Flows and State Machine

## 6.1 Booking state machine

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT : createBooking
    PENDING_PAYMENT --> CONFIRMED : payForBooking (success)
    PENDING_PAYMENT --> PENDING_PAYMENT : payment declined (retry)
    PENDING_PAYMENT --> CANCELLED : customer cancels / 15-min expiry
    CONFIRMED --> ONGOING : markPickup (on/after start date)
    CONFIRMED --> CANCELLED : customer cancels (+ refund)
    ONGOING --> COMPLETED : markReturn (+ late fee if any)
    COMPLETED --> [*]
    CANCELLED --> [*]
```

| From \ To | PENDING_PAYMENT | CONFIRMED | ONGOING | COMPLETED | CANCELLED |
|---|---|---|---|---|---|
| PENDING_PAYMENT | – | ✅ | ❌ | ❌ | ✅ |
| CONFIRMED | ❌ | – | ✅ | ❌ | ✅ |
| ONGOING | ❌ | ❌ | – | ✅ | ❌ |
| COMPLETED | ❌ | ❌ | ❌ | – | ❌ |
| CANCELLED | ❌ | ❌ | ❌ | ❌ | – |

Blocking statuses (vehicle is held for the dates): **PENDING_PAYMENT, CONFIRMED, ONGOING**.

## 6.2 Search → book → pay

```mermaid
sequenceDiagram
    actor C as Customer
    participant VC as VehicleController
    participant VS as VehicleService
    participant BC as BookingController
    participant BS as BookingService
    participant LM as VehicleLockManager
    participant BR as BookingRepository
    participant PR as PricingStrategy
    participant PS as PaymentService
    participant F as PaymentProcessorFactory

    C->>VC: search(branch, start, end, filters)
    VC->>VS: searchAvailable(request)
    VS->>BR: existsActiveOverlap(vehicle, start, end) for each vehicle
    VS-->>C: available vehicles (cheapest first)

    C->>BC: create(userId, vehicleId, start, end)
    BC->>BS: createBooking(request)
    BS->>BS: validate user role and dates
    BS->>LM: withLock(vehicleId)
    activate LM
    LM->>BR: existsActiveOverlap(...)  (re-check, search may be stale)
    alt overlap
        LM-->>C: 409 VehicleNotAvailable
    else free
        LM->>PR: calculatePrice(vehicle, start, end)
        LM->>BR: save(Booking PENDING_PAYMENT)
    end
    deactivate LM
    BS-->>C: 201 booking (PENDING_PAYMENT)

    C->>BC: pay(bookingId, UPI)
    BC->>BS: payForBooking(request)
    BS->>LM: withLock(vehicleId)
    BS->>PS: charge(bookingId, total, UPI, RENTAL)
    PS->>F: getProcessor(UPI)
    F-->>PS: UpiPaymentProcessor
    PS-->>BS: Payment (SUCCESS / FAILED)
    alt SUCCESS
        BS->>BS: booking.confirm()
        BS-->>C: 201 payment, booking CONFIRMED
    else FAILED
        BS-->>C: 402 PaymentFailed (booking stays PENDING_PAYMENT)
    end
```

## 6.3 Pickup and return

```mermaid
sequenceDiagram
    actor A as Admin
    participant BS as BookingService
    participant LF as LateFeePolicy
    participant PS as PaymentService

    A->>BS: markPickup(adminId, bookingId)
    BS->>BS: requireAdmin, status == CONFIRMED, today in [start, end], vehicle rentable
    BS->>BS: booking.startTrip(now)  → ONGOING

    A->>BS: markReturn(adminId, bookingId, returnTime, method)
    BS->>BS: status == ONGOING, returnTime >= pickupTime
    BS->>LF: calculateLateFee(vehicle, endDate, returnTime)
    alt late fee > 0
        BS->>PS: charge(bookingId, fee, method, LATE_FEE)
    end
    BS->>BS: booking.completeTrip(returnTime, fee) → COMPLETED
```

**Late fee example:** Creta at ₹3,200/day, booked 5–7 Nov, returned 9 Nov 11:00 → 2 late days × 3,200 × 1.5 = **₹9,600**; total = 9,600 + 9,600 = ₹19,200.

## 6.4 Cancel and refund

```mermaid
flowchart TD
    A[cancelBooking bookingId, userId] --> B{Owner or admin?}
    B -- no --> X[403 AccessDenied]
    B -- yes --> C[lock vehicle]
    C --> D{Status?}
    D -- PENDING_PAYMENT --> E[cancel]
    D -- CONFIRMED --> F[cancel + refund RENTAL payments]
    D -- ONGOING / COMPLETED / CANCELLED --> Y[409 InvalidBookingState]
    E --> G[publish CANCELLED]
    F --> G
```

## 6.5 Unpaid booking expiry

Every minute (`PendingBookingCleanupJob`), bookings that are still `PENDING_PAYMENT` 15 minutes after creation are cancelled. The status is **re-checked under the vehicle lock**, because the customer may have paid a millisecond earlier.

## 6.6 Concurrency: preventing double booking

**The race without a lock:**

```mermaid
sequenceDiagram
    participant T1 as Thread A (Riya)
    participant T2 as Thread B (Arjun)
    participant DB as Bookings
    T1->>DB: any overlap for Swift 10–12? → no
    T2->>DB: any overlap for Swift 10–12? → no
    T1->>DB: save booking
    T2->>DB: save booking
    Note over DB: Swift booked twice ❌
```

**With `VehicleLockManager`:** the check and the save for one vehicle run as one atomic step. Thread B waits for A, then sees A's booking and gets 409. Bookings for *different* vehicles still run in parallel because each vehicle has its own lock.

Proven by `testConcurrentBookingsForSameVehicleOnlyOneWins` (32 threads, exactly 1 success, 31 conflicts) and section 6 of the demo.

**Options in a database-backed version:**

| Approach | How | Trade-off |
|---|---|---|
| Pessimistic lock | `SELECT ... FROM vehicle WHERE id = ? FOR UPDATE` inside `@Transactional`, then check overlap and insert | Simple, correct, serialises bookings per vehicle |
| Optimistic lock | `@Version` on vehicle, bump it on every booking; retry on `OptimisticLockException` | No waiting, but retries under contention |
| DB constraint | PostgreSQL exclusion constraint on `(vehicle_id, daterange)` | Strongest guarantee; DB-specific |
| Distributed lock | Redis lock keyed by vehicle id | Needed only if logic spans services |

A JVM lock (this project) only works for a **single instance**. Once you run two app servers, move to one of the database options.
