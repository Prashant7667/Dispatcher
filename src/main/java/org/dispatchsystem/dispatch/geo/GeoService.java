package org.dispatchsystem.dispatch.geo;

import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.Ride;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class GeoService {
    private final DriverRepository driverRepository;
    GeoService(DriverRepository driverRepository){
        this.driverRepository=driverRepository;
    }
    public List<Driver> findNearbyAvailableDrivers(Ride ride, double radiusKm) {
        // Get all available drivers (in production, use a spatial index!)
        List<Driver> availableDrivers = driverRepository
                .findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return availableDrivers.stream()
                .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
                .filter(d -> haversineDistance(ride.getStartLatitude(), ride.getStartLongitude(), d.getLatitude(), d.getLongitude()) <= radiusKm)
                .filter(d -> supportsBookingType(d, ride.getBookingType()))
                .filter(d -> supportsDuration(d, ride.getEstimatedDurationMinutes()))
                .filter(d -> isAvailableForSchedule(d, ride.getScheduledStart()))
                .collect(Collectors.toList());
    }

    private boolean supportsBookingType(Driver driver, BookingType bookingType) {
        if (bookingType == null) {
            return true;
        }
        if (driver.getSupportedBookingTypes() == null || driver.getSupportedBookingTypes().isEmpty()) {
            return bookingType == BookingType.TRIP;
        }
        return driver.getSupportedBookingTypes().stream().anyMatch(type -> type.name().equals(bookingType.name()));
    }

    private boolean supportsDuration(Driver driver, Integer estimatedDurationMinutes) {
        if (estimatedDurationMinutes == null || driver.getMaxRentalDurationMinutes() == null) {
            return true;
        }
        return estimatedDurationMinutes <= driver.getMaxRentalDurationMinutes();
    }

    private boolean isAvailableForSchedule(Driver driver, LocalDateTime scheduledStart) {
        if (scheduledStart == null) {
            return true;
        }
        if (driver.getAvailableFrom() != null && scheduledStart.isBefore(driver.getAvailableFrom())) {
            return false;
        }
        if (driver.getAvailableUntil() != null && scheduledStart.isAfter(driver.getAvailableUntil())) {
            return false;
        }
        return true;
    }

    public double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
