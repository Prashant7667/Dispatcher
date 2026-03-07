package org.dispatchsystem.dispatch.orchestrator;

import org.dispatchsystem.dispatch.geo.GeoService;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DispatchOrchestrator {
    private final GeoService geoService;
    private final OfferManager offerManager;
    private final RideRepository rideRepository;
    DispatchOrchestrator(GeoService geoService, OfferManager offerManager, RideRepository rideRepository){
        this.geoService = geoService;
        this.offerManager = offerManager;
        this.rideRepository = rideRepository;
    }
    private List<Driver> rankByDistance(List<Driver> drivers, double lat, double lon) {
        return drivers.stream()
                .sorted(Comparator.comparingDouble(d ->
                        geoService.haversineDistance(lat, lon, d.getLatitude(), d.getLongitude())))
                .collect(Collectors.toList());
    }
    public void dispatch(Ride ride){
        List<Driver> nearbyDrivers = geoService.findNearbyAvailableDrivers(ride.getStartLatitude(), ride.getStartLongitude(), 5.0);
        if (nearbyDrivers.isEmpty()) {
            ride.setStatus(RideStatus.CANCELLED);
            rideRepository.save(ride);
            // TODO: notify rider "no drivers available"
            return;
        }
        // Step 2: Rank drivers (for now just sort by distance)
        // In Phase 2, this becomes DecisionEngine.rankDrivers(ride, nearbyDrivers)
        List<Driver> rankedDrivers = rankByDistance(
                nearbyDrivers,
                ride.getStartLatitude(),
                ride.getStartLongitude()
        );
        // Step 3: Start the offer flow — try drivers one by one
        ride.setStatus(RideStatus.DISPATCHING);
        rideRepository.save(ride);
        offerManager.startOfferFlow(ride, rankedDrivers);
    }
}
