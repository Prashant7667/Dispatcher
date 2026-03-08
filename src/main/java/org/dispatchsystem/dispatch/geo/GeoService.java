package org.dispatchsystem.dispatch.geo;

import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
public class GeoService {
    private final DriverRepository driverRepository;
    GeoService(DriverRepository driverRepository){
        this.driverRepository=driverRepository;
    }
    public List<Driver> findNearbyAvailableDrivers(double lat, double lon, double radiusKm) {
        // Get all available drivers (in production, use a spatial index!)
        List<Driver> availableDrivers = driverRepository
                .findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return availableDrivers.stream()
                .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
                .filter(d -> haversineDistance(lat, lon, d.getLatitude(), d.getLongitude()) <= radiusKm)
                .collect(Collectors.toList());
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
