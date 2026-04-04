# Frontend API Handoff

This is the current backend contract after the auth and websocket cleanup.

## Base setup

- Base URL: `http://localhost:8080`
- Content type: `application/json`
- REST auth: `Authorization: Bearer <token>`
- WebSocket auth: query string token, for example `ws://localhost:8080/ws/users?token=<jwt>`

## Roles

Backend now uses these roles consistently:

- `USER`
- `DRIVER`
- `ADMIN`

Notes:

- Passenger flows use `USER` everywhere now.
- `ADMIN` is now a first-class stored role on user accounts.
- Driver accounts are still separate from user/admin accounts.

## Common error shape

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "requestedVehicleClass: requestedVehicleClass is required",
  "timestamp": "2026-04-04T14:12:33.456"
}
```

## Enums

### UserRole

`USER`, `ADMIN`

### VehicleClass

`BIKE`, `AUTO`, `HATCHBACK`, `SEDAN`, `SUV`, `PREMIUM`, `VAN`

### BookingType / DriverBookingType

`TRIP`, `HOURLY`, `DAILY`, `MONTHLY`

### RentalPlan

`NONE`, `FLEXIBLE`, `WEEKEND`, `WEEKLY`, `MONTHLY_STANDARD`

### RideStatus

`REQUESTED`, `SCHEDULED`, `DISPATCHING`, `DRIVER_ASSIGNED`, `DRIVER_EN_ROUTE`, `DRIVER_ARRIVED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

### AvailabilityStatus

`AVAILABLE`, `RESERVED`, `UNAVAILABLE`

### UserNotificationType

`DRIVER_ASSIGNED`, `NO_DRIVERS_AVAILABLE`, `DRIVER_EN_ROUTE`, `DRIVER_ARRIVED`, `RIDE_CANCELLED`

### Driver offer response status

Frontend sends:

- `ACCEPTED`
- `REJECTED`
- `PENDING`
- `EXPIRED`
- `CANCELLED`

Important:

- Driver websocket replies now work directly from `type`.
- Frontend can send only `rideId` and `type`.

## Core object shapes

### UserResponseDTO

```json
{
  "id": 1,
  "name": "Aman",
  "email": "aman@example.com",
  "phoneNumber": "9999999999",
  "role": "USER"
}
```

### VehicleDetails

```json
{
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
}
```

### DriverResponseDTO

```json
{
  "id": 10,
  "name": "Nisha",
  "email": "driver1@dispatch.dev",
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
  "avgRating": 4.8,
  "totalRating": 52,
  "supportedBookingTypes": ["TRIP", "HOURLY"],
  "availableFrom": "2026-04-04T09:00:00",
  "availableUntil": "2026-04-04T21:00:00",
  "maxRentalDurationMinutes": 240,
  "availabilityStatus": "AVAILABLE"
}
```

### RideResponseDTO

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

## Auth APIs

### Register user or admin

- Method: `POST`
- Path: `/users`
- Auth: public

Request:

```json
{
  "name": "Aman",
  "email": "aman@example.com",
  "password": "secret",
  "phoneNumber": "9999999999",
  "role": "USER"
}
```

Role rules:

- `role` is optional
- if omitted, backend stores `USER`
- admin accounts can be created with `role: "ADMIN"`

Response: `201 Created`

```json
{
  "id": 1,
  "name": "Aman",
  "email": "aman@example.com",
  "phoneNumber": "9999999999",
  "role": "USER"
}
```

### Login

- Method: `POST`
- Path: `/users/login`
- Auth: public

Request:

```json
{
  "email": "aman@example.com",
  "password": "secret"
}
```

Response:

```json
{
  "token": "<jwt>",
  "role": "USER"
}
```

Possible login roles:

- `USER`
- `DRIVER`
- `ADMIN`

## User APIs

### Get all users

- Method: `GET`
- Path: `/users/all`
- Auth: bearer token
- Response: `UserResponseDTO[]`

### Get current user

- Method: `GET`
- Path: `/users/me/details`
- Auth: bearer token
- Response: `UserResponseDTO`

### Update current user

- Method: `PUT`
- Path: `/users/me/update`
- Auth: bearer token

Request:

```json
{
  "name": "Updated Name",
  "password": "new-secret",
  "phoneNumber": "8888888888"
}
```

All fields optional.

Response: `UserResponseDTO`

### Delete current user

- Method: `DELETE`
- Path: `/users/me/delete`
- Auth: bearer token
- Response: `204 No Content`

## Driver APIs

### Create driver

- Method: `POST`
- Path: `/drivers`
- Auth: public

Request:

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

Notes:

- required fields are `name`, `email`, `password`, `phoneNumber`, `vehicleDetails`, `latitude`, `longitude`
- if `supportedBookingTypes` is omitted, backend defaults to `["TRIP"]`

Response: `DriverResponseDTO`

### Get all drivers

- Method: `GET`
- Path: `/drivers`
- Auth: bearer token
- Response: `DriverResponseDTO[]`

### Get current driver

- Method: `GET`
- Path: `/drivers/me`
- Auth: bearer token
- Response: `DriverResponseDTO`

### Update current driver

- Method: `PUT`
- Path: `/drivers/me`
- Auth: bearer token

Request:

```json
{
  "name": "Driver Updated",
  "password": "new-secret",
  "phoneNumber": "8888888888",
  "vehicleDetails": {
    "vehicleClass": "SUV",
    "seatCapacity": 6,
    "luggageCapacityKg": 40,
    "city": "Bengaluru",
    "zone": "North",
    "vehicleMake": "Toyota",
    "vehicleModel": "Innova",
    "vehicleColor": "Black",
    "licensePlate": "KA01CD5678",
    "vehicleYear": 2025
  },
  "latitude": 12.99,
  "longitude": 77.61,
  "supportedBookingTypes": ["TRIP", "DAILY"],
  "availableFrom": "2026-04-04T08:00:00",
  "availableUntil": "2026-04-04T22:00:00",
  "maxRentalDurationMinutes": 480,
  "availabilityStatus": "AVAILABLE"
}
```

Response: `DriverResponseDTO`

### Update driver availability

- Method: `PATCH`
- Path: `/drivers/me/availability?status=AVAILABLE`
- Auth: bearer token
- Response: `DriverResponseDTO`

### Delete current driver

- Method: `DELETE`
- Path: `/drivers`
- Auth: bearer token
- Response: `204 No Content`

## Ride APIs

### Request ride

- Method: `POST`
- Path: `/rides/request`
- Auth: bearer token with `USER` role

Request:

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

Rules:

- `requestedVehicleClass` is required
- `requiredLuggageCapacity` must be `>= 0`
- `estimatedDurationMinutes` must be positive when sent
- if `rentalPlan` is omitted, backend stores `NONE`
- if `scheduledStart` is future time, status starts as `SCHEDULED`

Response: `RideResponseDTO`

### Get ride by id

- Method: `GET`
- Path: `/rides/{id}`
- Auth: bearer token
- Response: `RideResponseDTO`

### Cancel ride

- Method: `POST`
- Path: `/rides/me/{rideId}/cancel`
- Auth: bearer token with `USER` role
- Response: `RideResponseDTO`

### Update ride

- Method: `PUT`
- Path: `/rides/{id}`
- Auth: bearer token with `ADMIN` role
- Request body: same as ride request
- Response: `RideResponseDTO`

### Driver ride status updates

- `POST /rides/me/{rideId}/en-route`
- `POST /rides/me/{rideId}/arrived`
- `POST /rides/me/{rideId}/start`
- `POST /rides/me/{rideId}/complete`

All require bearer token with `DRIVER` role and all return `RideResponseDTO`.

### Ride history

- `GET /rides/me/user/rideHistory`
  auth: bearer token with `USER` role
  response: `RideResponseDTO[]`
- `GET /rides/me/driver/rideHistory`
  auth: bearer token with `DRIVER` role
  response: `RideResponseDTO[]`

## Admin APIs

All admin routes require bearer token with `ADMIN` role.

Query params `from` and `to` use `LocalDateTime`, for example:

`/admin/analytics/dispatch-summary?from=2026-03-30T01:00:00&to=2026-03-30T07:00:00`

### Dispatch timeline

- Method: `GET`
- Path: `/admin/rides/{rideId}/dispatch-timeline`
- Response: array of dispatch timeline events

### Dispatch explanation

- Method: `GET`
- Path: `/admin/rides/{rideId}/dispatch-explanation`
- Response: dispatch explanation object with selected/skipped driver reasoning

### Dispatch summary

- Method: `GET`
- Path: `/admin/analytics/dispatch-summary`
- Response:

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

### Failure reasons

- Method: `GET`
- Path: `/admin/analytics/failure-reasons`
- Response:

```json
[
  {
    "reasonCode": "NO_ELIGIBLE_DRIVERS",
    "count": 4
  }
]
```

### Outcome breakdown

- Method: `GET`
- Path: `/admin/analytics/outcomes`
- Response:

```json
{
  "successfulAssignments": 3,
  "failedNoEligibleDrivers": 4,
  "failedAllOffersExhausted": 1,
  "cancelledByPassenger": 2,
  "offerRejectedCount": 5,
  "offerTimeoutCount": 3
}
```

### Ops summary

- Method: `GET`
- Path: `/admin/analytics/ops-summary`
- Response includes acceptance rate, average dispatch time, top reasons, low-supply zones, and AI narrative.

## WebSocket contract

### User socket

- URL: `ws://localhost:8080/ws/users?token=<jwt>`
- JWT role must be `USER`

Payload from backend:

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

Important:

- user websocket payload is now a clean DTO
- no raw entity is sent
- no nested user object or password is exposed

### Driver socket

- URL: `ws://localhost:8080/ws/drivers?token=<jwt>`
- JWT role must be `DRIVER`

Offer payload from backend:

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
  "createdAt": "2026-04-04T14:20:00",
  "scheduledStart": null,
  "fare": 245.5,
  "userId": 3,
  "userName": "Aman"
}
```

Driver reply payload from frontend:

```json
{
  "type": "ACCEPTED",
  "rideId": 44
}
```

or

```json
{
  "type": "REJECTED",
  "rideId": 44
}
```

Ack payload from backend:

```json
{
  "offerStatus": "SUCCESS",
  "rideId": 44,
  "message": "Ride is Accepted by the driver"
}
```

Possible `offerStatus` values:

- `SUCCESS`
- `OFFER_EXPIRED`
- `RIDE_CANCELLED`
- `NO_ACTIVE_OFFER`
- `REJECTED`
- `ERROR`

## What changed for frontend

These are the important updates versus the earlier backend state:

1. Passenger role mismatch is fixed. Use `USER` for all passenger flows.
2. `/users/login` is public now.
3. `/drivers/**` is no longer broadly open. Frontend should send bearer token for driver fetch/update flows.
4. `ADMIN` is now a stored role on user accounts and works in auth decisions.
5. `UserResponseDTO` now includes `role`.
6. User registration can optionally create an admin account by sending `role: "ADMIN"`.
7. Websocket payloads are now DTO-based, not raw entities.

## New frontend notes

- If frontend needs admin login, create an admin user record first with:

```json
{
  "name": "Ops Admin",
  "email": "admin@example.com",
  "password": "secret",
  "phoneNumber": "9999999999",
  "role": "ADMIN"
}
```

- For driver websocket replies, frontend can now send only `type` plus `rideId`.
- Date-time format is still plain `LocalDateTime` JSON string, for example `2026-04-04T15:00:00`.
- There is still no pagination on list APIs.
