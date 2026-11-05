# 7. API Design

The API is **live**: start `ApiServerApplication` (or `server.bat`) and it serves these endpoints on http://localhost:8080, with the OpenAPI 3.1 contract at `/openapi.yaml` and Swagger UI at `/swagger-ui`. How to test it: **[10-api-testing-guide.md](10-api-testing-guide.md)**.

The acting user is sent in the **`X-User-Id`** header (a stand-in for a JWT). All responses use the `ApiResponse` envelope, which includes a `traceId` (also in the `X-Trace-Id` header).

```json
{ "status": 201, "success": true, "message": "Created", "data": { ... }, "traceId": "6e03dea2" }
{ "status": 409, "success": false, "message": "Vehicle 3 is already booked between 2026-11-11 and 2026-11-14", "data": null, "traceId": "7d8a0a17" }
```

## 7.1 Endpoints

| Method | Path | Who | Controller method | Success |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | anyone | `AuthController.register` | 201 |
| POST | `/api/v1/auth/login` | anyone | `AuthController.login` | 200 |
| POST | `/api/v1/branches` | admin | `BranchController.create` | 201 |
| GET | `/api/v1/branches` | anyone | `BranchController.list` | 200 |
| GET | `/api/v1/branches/{id}/vehicles` | anyone | `VehicleController.listByBranch` | 200 |
| POST | `/api/v1/vehicles/cars` | admin | `VehicleController.addCar` | 201 |
| POST | `/api/v1/vehicles/bikes` | admin | `VehicleController.addBike` | 201 |
| PATCH | `/api/v1/vehicles/{id}/status` | admin | `VehicleController.changeStatus` | 200 |
| GET | `/api/v1/vehicles/search?branchId=&start=&end=&category=&carType=&minSeats=` | anyone | `VehicleController.search` | 200 |
| POST | `/api/v1/bookings` | customer | `BookingController.create` | 201 |
| POST | `/api/v1/bookings/{id}/payments` | owner | `BookingController.pay` | 201 |
| POST | `/api/v1/bookings/{id}/cancel` | owner / admin | `BookingController.cancel` | 200 |
| POST | `/api/v1/bookings/{id}/pickup` | admin | `BookingController.pickup` | 200 |
| POST | `/api/v1/bookings/{id}/return` | admin | `BookingController.returnVehicle` | 200 |
| GET | `/api/v1/bookings/{id}` | owner / admin | `BookingController.get` | 200 |
| GET | `/api/v1/bookings/{id}/payments` | owner / admin | `BookingController.payments` | 200 |
| GET | `/api/v1/users/{userId}/bookings` | owner | `BookingController.history` | 200 |

Why `POST /bookings/{id}/cancel` and not `DELETE /bookings/{id}`? A cancelled booking is not deleted: it stays in history with a reason and may have a refund. It is a **state change**, so it is modelled as an action.

## 7.2 Sample requests

**Create booking**
```http
POST /api/v1/bookings
X-User-Id: 2
{ "vehicleId": 3, "startDate": "2026-11-10", "endDate": "2026-11-12" }
```
```json
{ "status": 201, "success": true, "message": "Created",
  "data": { "id": 2, "userId": 2, "vehicleId": 3, "startDate": "2026-11-10", "endDate": "2026-11-12",
            "baseAmount": 8400.00, "lateFee": 0, "totalAmount": 8400.00, "status": "PENDING_PAYMENT" } }
```

**Pay**
```http
POST /api/v1/bookings/2/payments
X-User-Id: 2
{ "method": "UPI" }
```

**Return (late)**
```http
POST /api/v1/bookings/1/return
X-User-Id: 1
{ "returnTime": "2026-11-09T11:00:00", "lateFeeMethod": "CARD" }
```

## 7.3 Error codes

| Status | Exception | Typical cause |
|---|---|---|
| 400 | `InvalidRequestException` | Blank field, bad email, past start date, rental > 30 days, pickup before start |
| 401 | `AuthenticationException` | Wrong email or password |
| 402 | `PaymentFailedException` | Gateway declined (e.g. UPI limit) |
| 403 | `AccessDeniedException` | Customer calling an admin action; acting on someone else's booking |
| 404 | `ResourceNotFoundException` | Unknown user / branch / vehicle / booking id |
| 409 | `VehicleNotAvailableException` | Overlapping booking or vehicle out of service |
| 409 | `DuplicateResourceException` | Email or registration number already exists |
| 409 | `InvalidBookingStateException` | Illegal transition (pickup a cancelled booking, pay twice) |
| 405 | (router) | Wrong HTTP method for a known path; the `Allow` header lists the valid ones |
| 500 | anything else | A bug; details are logged, not returned |


In a real API, the acting user would come from a verified JWT instead of the `X-User-Id` header.
