# Frontend API Summary

This is the short version to forward to frontend.

## Base

- Base URL: `http://localhost:8080`
- REST auth: `Authorization: Bearer <token>`
- WebSocket auth: `ws://localhost:8080/ws/...?...token=<jwt>`

## Roles

- Passenger role is `USER`
- Driver role is `DRIVER`
- Admin role is `ADMIN`

## Auth

- `POST /users` creates `USER` by default
- `POST /users` can also create admin with `role: "ADMIN"`
- `POST /users/login` is public
- Login response is:

```json
{
  "token": "<jwt>",
  "role": "USER"
}
```

## Main APIs

- Users:
  `GET /users/me/details`
  `PUT /users/me/update`
  `DELETE /users/me/delete`
- Drivers:
  `POST /drivers`
  `GET /drivers`
  `GET /drivers/me`
  `PUT /drivers/me`
  `PATCH /drivers/me/availability?status=AVAILABLE`
- Rides:
  `POST /rides/request`
  `GET /rides/{id}`
  `POST /rides/me/{rideId}/cancel`
  `GET /rides/me/user/rideHistory`
  `GET /rides/me/driver/rideHistory`
- Admin:
  `/admin/rides/{rideId}/dispatch-timeline`
  `/admin/rides/{rideId}/dispatch-explanation`
  `/admin/analytics/dispatch-summary`
  `/admin/analytics/failure-reasons`
  `/admin/analytics/outcomes`
  `/admin/analytics/ops-summary`

## Important request fields

- Ride request requires:
  `startLongitude`, `startLatitude`, `endLongitude`, `endLatitude`, `bookingType`, `requestedVehicleClass`, `requiredLuggageCapacity`
- Driver create requires:
  `name`, `email`, `password`, `phoneNumber`, `vehicleDetails`, `latitude`, `longitude`
- `UserResponseDTO` now includes `role`

## WebSockets

- User socket:
  `ws://localhost:8080/ws/users?token=<jwt>`
- Driver socket:
  `ws://localhost:8080/ws/drivers?token=<jwt>`

Driver offer payload from backend is now clean DTO data only.

Driver reply payload from frontend is now:

```json
{
  "rideId": 44,
  "type": "ACCEPTED"
}
```

or

```json
{
  "rideId": 44,
  "type": "REJECTED"
}
```

User notification payload is also now DTO-based and no longer exposes raw entity data.

## Things frontend should know

- Use `USER` everywhere for passenger flows
- Admin flows require a logged-in `ADMIN`
- Date-time fields use `LocalDateTime` strings like `2026-04-04T15:00:00`
- No pagination is implemented yet
- Full detailed contract is in [frontend-api-handoff.md](/Users/prashantpandey/IdeaProjects/Dispatch-System/docs/frontend-api-handoff.md)
