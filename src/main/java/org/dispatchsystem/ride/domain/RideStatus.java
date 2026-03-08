package org.dispatchsystem.ride.domain;

public enum RideStatus {
    REQUESTED,
    DISPATCHING,
    DRIVER_ASSIGNED,
    DRIVER_EN_ROUTE,
    DRIVER_ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}