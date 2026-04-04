# Dispatcher

A backend ride-dispatching engine built with Java and Spring Boot. Dispatcher handles the full lifecycle of a ride — from passenger booking through real-time driver matching, offer management, and ride completion — exposing a clean REST + WebSocket API for frontend clients.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [Roles](#roles)
- [API Reference](#api-reference)
  - [Auth](#auth-apis)
  - [Users](#user-apis)
  - [Drivers](#driver-apis)
  - [Rides](#ride-apis)
  - [Admin & Analytics](#admin-apis)
- [WebSocket Contract](#websocket-contract)
  - [User Socket](#user-socket)
  - [Driver Socket](#driver-socket)
- [Enums & Data Models](#enums--data-models)
- [Error Handling](#error-handling)

---

## Overview

Dispatcher is a ride-hailing backend that manages:

- **Passenger flows** — registration, login, ride requests, cancellations, and ride history
- **Driver flows** — onboarding with vehicle details, availability management, real-time offer acceptance/rejection
- **Dispatch engine** — smart driver matching based on vehicle class, luggage capacity, location, booking type, and availability
- **Admin analytics** — dispatch timelines, outcome breakdowns, failure reason tracking, and AI-generated ops narratives
- **Real-time communication** — WebSocket channels for passengers and drivers with clean DTO-based payloads

---

## Architecture

```
Client (REST + WebSocket)
        │
        ▼
  Spring Boot App
  ┌─────────────────────────────┐
  │  Auth Layer (JWT)           │
  │  REST Controllers           │
  │  WebSocket Handlers         │
  │  Dispatch Engine            │
  │  Analytics Service          │
  └──────────────┬──────────────┘
                 │
           Database (JPA)
```

The dispatch engine sends ride offers to eligible drivers over WebSocket. Drivers accept or reject in real time. If a driver times out or rejects, the engine moves to the next candidate.

---

## Tech Stack

- **Language**: Java
- **Framework**: Spring Boot
- **Build tool**: Gradle
- **Auth**: JWT (Bearer tokens)
- **Real-time**: WebSocket
- **Persistence**: JPA / Hibernate

---

## Getting Started

### Prerequisites

- Java 17+
- Gradle (or use the included `./gradlew` wrapper)

### Run locally

```bash
git clone https://github.com/Prashant7667/Dispatcher.git
cd Dispatcher
./gradlew bootRun
```

The server starts at `http://localhost:8080`.

### Build a JAR

```bash
./gradlew build
java -jar build/libs/Dispatcher-*.jar
```

---

## Authentication

- **REST APIs**: Pass a JWT in the `Authorization` header.
  ```
  Authorization: Bearer <token>
  ```
- **WebSocket**: Pass the JWT as a query parameter.
  ```
  ws://localhost:8080/ws/users?token=<jwt>
  ws://localhost:8080/ws/drivers?token=<jwt>
  ```

Tokens are obtained from the `/users/login` endpoint. The login response includes both the token and the role, which determines what the client can do.

---

## Roles

| Role    | Description                                               |
|---------|-----------------------------------------------------------|
| `USER`  | Passenger — can request rides, view history, cancel rides |
| `DRIVER`| Driver — receives offers, updates ride status             |
| `ADMIN` | Operator — full access including analytics endpoints      |

> **Note**: All passenger flows use the `USER` role. `ADMIN` is a stored role on user accounts and is set at registration.

---

## API Reference

### Base URL

```
http://localhost:8080
```

All request/response bodies use `Content-Type: application/json`.

---

### Auth APIs

#### Register user or admin

```
POST /users
Public — no auth required
```

**Request**
```json
{
  "name": "Aman",
  "email": "aman@example.com",
  "password": "secret",
  "phoneNumber": "9999999999",
  "role": "USER"
}
```

- `role` is optional. Defaults to `USER` if omitted.
- Send `"role": "ADMIN"` to create an admin account.

**Response** `201 Created`
```json
{
  "id": 1,
  "name": "Aman",
  "email": "aman@example.com",
  "phoneNumber": "9999999999",
  "role": "USER"
}
```

---

#### Login

```
POST /users/login
Public — no auth required
```

**Request**
```json
{
  "email": "aman@example.com",
  "password": "secret"
}
```

**Response**
```json
{
  "token": "<jwt>",
  "role": "USER"
}
```

The `role` field will be `USER`, `DRIVER`, or `ADMIN`. Use this to determine which WebSocket endpoint and UI flows to use.

---

### User APIs

All user routes require a Bearer token.

| Method   | Path                  | Description              |
|----------|-----------------------|--------------------------|
| `GET`    | `/users/all`          | Get all users (admin)    |
| `GET`    | `/users/me/details`   | Get current user profile |
| `PUT`    | `/users/me/update`    | Update profile           |
| `DELETE` | `/users/me/delete`    | Delete account           |

**Update request** (all fields optional)
```json
{
  "name": "Updated Name",
  "password": "new-secret",
  "phoneNumber": "8888888888"
}
```

---

### Driver APIs

#### Create driver

```
POST /drivers
Public — no auth required
```

**Request**
```json
{
  "name": "Driver One",
  "email": "driver1@dispatch.dev",
  "password": "secret",
  "phoneNumber": "9999999998",
  "vehicleDetails": {
    "vehicleClass": "SEDAN",
    "seatCapacity": 4,
    "luggageCapacityKg": 20,
    "city": "Bengaluru",
    "zone": "South",
    "vehicleMake": "Hyundai",
    "vehicleModel": "i20",
    "vehicleColor": "White",
    "licensePlate": "KA01AB1234",
    "vehicleYear": 2024
  },
  "latitude": 12.9716,
  "longitude": 77.5946,
  "supportedBookingTypes": ["TRIP", "HOURLY"],
  "availableFrom": "2026-04-04T09:00:00",
  "availableUntil": "2026-04-04T21:00:00",
  "maxRentalDurationMinutes": 240
}
```

- Required: `name`, `email`, `password`, `phoneNumber`, `vehicleDetails`, `latitude`, `longitude`
- `supportedBookingTypes` defaults to `["TRIP"]` if omitted

**Response**: `DriverResponseDTO`

---

#### Other driver routes (require Bearer token)

| Method   | Path                              | Description                  |
|----------|-----------------------------------|------------------------------|
| `GET`    | `/drivers`                        | Get all drivers              |
| `GET`    | `/drivers/me`                     | Get current driver profile   |
| `PUT`    | `/drivers/me`                     | Update driver profile        |
| `PATCH`  | `/drivers/me/availability?status=AVAILABLE` | Update availability status |
| `DELETE` | `/drivers`                        | Delete driver account        |

---

### Ride APIs

#### Request a ride

```
POST /rides/request
Auth: USER role
```

**Request**
```json
{
  "startLongitude": 77.5946,
  "startLatitude": 12.9716,
  "endLongitude": 77.62,
  "endLatitude": 12.99,
  "bookingType": "TRIP",
  "scheduledStart": "2026-04-04T15:00:00",
  "estimatedDurationMinutes": 35,
  "rentalPlan": "NONE",
  "requestedVehicleClass": "SEDAN",
  "requiredLuggageCapacity": 15
}
```

- `requestedVehicleClass` is required
- `requiredLuggageCapacity` must be `>= 0`
- If `scheduledStart` is a future time, ride starts in `SCHEDULED` status
- `rentalPlan` defaults to `NONE` if omitted

**Response**: `RideResponseDTO`

---

#### Other ride routes

| Method | Path                              | Auth   | Description            |
|--------|-----------------------------------|--------|------------------------|
| `GET`  | `/rides/{id}`                     | Bearer | Get ride by ID         |
| `POST` | `/rides/me/{rideId}/cancel`       | USER   | Cancel a ride          |
| `PUT`  | `/rides/{id}`                     | ADMIN  | Update ride (admin)    |
| `GET`  | `/rides/me/user/rideHistory`      | USER   | User ride history      |
| `GET`  | `/rides/me/driver/rideHistory`    | DRIVER | Driver ride history    |

#### Driver status updates (DRIVER role)

```
POST /rides/me/{rideId}/en-route
POST /rides/me/{rideId}/arrived
POST /rides/me/{rideId}/start
POST /rides/me/{rideId}/complete
```

All return a `RideResponseDTO`.

---

### Admin APIs

All admin routes require a Bearer token with `ADMIN` role.

Date-time query params use `LocalDateTime` format: `2026-03-30T01:00:00`

| Method | Path                                      | Description                        |
|--------|-------------------------------------------|------------------------------------|
| `GET`  | `/admin/rides/{rideId}/dispatch-timeline` | Full event timeline for a ride     |
| `GET`  | `/admin/rides/{rideId}/dispatch-explanation` | Why each driver was selected/skipped |
| `GET`  | `/admin/analytics/dispatch-summary`       | Aggregate dispatch metrics         |
| `GET`  | `/admin/analytics/failure-reasons`        | Breakdown of dispatch failure codes |
| `GET`  | `/admin/analytics/outcomes`               | Assignment and rejection counts    |
| `GET`  | `/admin/analytics/ops-summary`            | Full ops report with AI narrative  |

**Dispatch summary response example**
```json
{
  "totalRides": 12,
  "totalOffersSent": 8,
  "acceptedOffers": 3,
  "rejectedOffers": 2,
  "timedOutOffers": 3,
  "cancelledRides": 4,
  "acceptanceRate": 37.5,
  "rejectionRate": 25.0,
  "timeoutRate": 37.5,
  "cancellationRate": 33.33,
  "averageDispatchTimeSeconds": 48.25,
  "driverUtilizationRate": 66.67
}
```

---

## WebSocket Contract

### User Socket

```
ws://localhost:8080/ws/users?token=<jwt>
JWT role must be USER
```

The backend pushes notifications to the passenger when the ride status changes.

**Payload shape from backend**
```json
{
  "type": "DRIVER_ASSIGNED",
  "message": "Driver Nisha has been assigned to your ride",
  "rideId": 44,
  "rideStatus": "DRIVER_ASSIGNED",
  "driverName": "Nisha",
  "driverId": 9,
  "ride": {
    "id": 44,
    "startLongitude": 77.5946,
    "startLatitude": 12.9716,
    "endLongitude": 77.62,
    "endLatitude": 12.99,
    "requestedVehicleClass": "SEDAN",
    "requiredLuggageCapacity": 15,
    "status": "DRIVER_ASSIGNED",
    "bookingType": "TRIP",
    "estimatedDurationMinutes": 35,
    "rentalPlan": "NONE",
    "scheduledStart": null,
    "fare": 245.5
  }
}
```

**Notification types** (`UserNotificationType`):

- `DRIVER_ASSIGNED`
- `NO_DRIVERS_AVAILABLE`
- `DRIVER_EN_ROUTE`
- `DRIVER_ARRIVED`
- `RIDE_CANCELLED`

---

### Driver Socket

```
ws://localhost:8080/ws/drivers?token=<jwt>
JWT role must be DRIVER
```

The backend pushes ride offers to the driver. The driver replies with an acceptance or rejection.

**Offer payload from backend**
```json
{
  "rideId": 44,
  "startLongitude": 77.5946,
  "startLatitude": 12.9716,
  "endLongitude": 77.62,
  "endLatitude": 12.99,
  "requestedVehicleClass": "SEDAN",
  "requiredLuggageCapacity": 15,
  "status": "DISPATCHING",
  "bookingType": "TRIP",
  "estimatedDurationMinutes": 35,
  "rentalPlan": "NONE",
  "fare": 245.5,
  "userId": 3,
  "userName": "Aman",
  "scheduledStart": null,
  "createdAt": "2026-04-04T14:20:00"
}
```

**Driver reply to backend**
```json
{ "type": "ACCEPTED", "rideId": 44 }
```
```json
{ "type": "REJECTED", "rideId": 44 }
```

**Ack payload from backend**
```json
{
  "offerStatus": "SUCCESS",
  "rideId": 44,
  "message": "Ride is Accepted by the driver"
}
```

**Possible `offerStatus` values**:

| Status               | Meaning                                        |
|----------------------|------------------------------------------------|
| `SUCCESS`            | Offer accepted and ride assigned               |
| `OFFER_EXPIRED`      | Driver took too long to respond                |
| `RIDE_CANCELLED`     | Passenger cancelled before driver responded    |
| `NO_ACTIVE_OFFER`    | No pending offer found for this ride           |
| `REJECTED`           | Driver rejected the offer                      |
| `ERROR`              | Unexpected server error                        |

---

## Enums & Data Models

### Enums

| Enum                  | Values                                                                                              |
|-----------------------|-----------------------------------------------------------------------------------------------------|
| `UserRole`            | `USER`, `ADMIN`                                                                                     |
| `VehicleClass`        | `BIKE`, `AUTO`, `HATCHBACK`, `SEDAN`, `SUV`, `PREMIUM`, `VAN`                                      |
| `BookingType`         | `TRIP`, `HOURLY`, `DAILY`, `MONTHLY`                                                                |
| `RentalPlan`          | `NONE`, `FLEXIBLE`, `WEEKEND`, `WEEKLY`, `MONTHLY_STANDARD`                                         |
| `RideStatus`          | `REQUESTED`, `SCHEDULED`, `DISPATCHING`, `DRIVER_ASSIGNED`, `DRIVER_EN_ROUTE`, `DRIVER_ARRIVED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `AvailabilityStatus`  | `AVAILABLE`, `RESERVED`, `UNAVAILABLE`                                                              |

### Core DTOs

**UserResponseDTO**
```json
{
  "id": 1,
  "name": "Aman",
  "email": "aman@example.com",
  "phoneNumber": "9999999999",
  "role": "USER"
}
```

**RideResponseDTO**
```json
{
  "id": 44,
  "startLongitude": 77.5946,
  "startLatitude": 12.9716,
  "endLongitude": 77.62,
  "endLatitude": 12.99,
  "bookingType": "TRIP",
  "scheduledStart": "2026-04-04T15:00:00",
  "estimatedDurationMinutes": 35,
  "rentalPlan": "NONE",
  "status": "DRIVER_ASSIGNED",
  "fare": 245.5,
  "driverId": 9,
  "driverName": "Nisha",
  "userId": 3,
  "userName": "Aman",
  "requestedVehicleClass": "SEDAN",
  "requiredLuggageCapacity": 15
}
```

---

## Error Handling

All API errors return a consistent shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "requestedVehicleClass: requestedVehicleClass is required",
  "timestamp": "2026-04-04T14:12:33.456"
}
```

---

## License

This project is open source. See [LICENSE](LICENSE) for details.
