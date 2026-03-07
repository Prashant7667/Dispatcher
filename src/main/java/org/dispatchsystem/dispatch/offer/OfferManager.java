package org.dispatchsystem.dispatch.offer;

import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.OfferStatus;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideOffer;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideOfferRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.ride.service.RideBroadcastService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Service
public class OfferManager {

    private final RideOfferRepository offerRepository;
    private final RideBroadcastService broadcastService;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    OfferManager(RideOfferRepository offerRepository, RideBroadcastService rideBroadcastService, RideRepository rideRepository, DriverRepository driverRepository){
        this.offerRepository = offerRepository;
        this.broadcastService = rideBroadcastService;
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
    }
    // Tracks which ride is on which offer attempt
    // Key: rideId → Value: the current state of the offer flow
    private final ConcurrentHashMap<Long, OfferFlowState> activeFlows = new ConcurrentHashMap<>();

    /**
     * Start the sequential offer flow for a ride.
     * Called by DispatchOrchestrator after ranking drivers.
     */
    public void startOfferFlow(Ride ride, List<Driver> rankedDrivers) {
        OfferFlowState state = new OfferFlowState(ride, rankedDrivers);
        activeFlows.put(ride.getId(), state);
        sendNextOffer(state);
    }

    /**
     * Send an offer to the next driver in the list.
     */
    private void sendNextOffer(OfferFlowState state) {
        if (state.getCurrentIndex() >= state.getRankedDrivers().size()) {
            // No more drivers to try
            handleNoDriversAvailable(state.getRide());
            return;
        }

        Driver nextDriver = state.getRankedDrivers().get(state.getCurrentIndex());

        // 1. Create offer record in DB
        RideOffer offer = new RideOffer();
        offer.setRide(state.getRide());
        offer.setDriver(nextDriver);
        offer.setStatus(OfferStatus.PENDING);
        offer.setSentAt(LocalDateTime.now());
        offer.setExpiresAt(LocalDateTime.now().plusSeconds(30));
        offerRepository.save(offer);

        state.setCurrentOffer(offer);

        // 2. Send WebSocket notification to this specific driver
        broadcastService.sendOfferToDriver(nextDriver, state.getRide());

        // 3. Schedule timeout — if no response in 30s, move to next
        ScheduledFuture<?> timeout = scheduler.schedule(
                () -> handleOfferTimeout(state),
                30, TimeUnit.SECONDS
        );
        state.setTimeoutFuture(timeout);
    }

    /**
     * Called when a driver responds via WebSocket.
     * This is triggered from RideSocketHandler when driver sends ACCEPT/REJECT.
     */
    public void handleDriverResponse(Long rideId, String driverEmail, String response) {
        OfferFlowState state = activeFlows.get(rideId);
        if (state == null) return; // ride already assigned or cancelled

        // Cancel the timeout since driver responded
        state.getTimeoutFuture().cancel(false);

        if ("ACCEPT".equals(response)) {
            // ✅ Driver accepted!
            RideOffer offer = state.getCurrentOffer();
            offer.setStatus(OfferStatus.ACCEPTED);
            offerRepository.save(offer);

            // Assign driver to ride
            Ride ride = state.getRide();
            ride.setDriver(offer.getDriver());
            ride.setStatus(RideStatus.DRIVER_ASSIGNED);
            rideRepository.save(ride);

            // Mark driver as unavailable
            Driver driver = offer.getDriver();
            driver.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
            driverRepository.save(driver);

            // Clean up
            activeFlows.remove(rideId);

            // TODO: Notify rider that driver was found

        } else if ("REJECT".equals(response)) {
            // ❌ Driver rejected — try next
            RideOffer offer = state.getCurrentOffer();
            offer.setStatus(OfferStatus.REJECTED);
            offerRepository.save(offer);

            state.incrementIndex();
            sendNextOffer(state);
        }
    }

    /**
     * Called when the 30-second timeout fires.
     */
    private void handleOfferTimeout(OfferFlowState state) {
        RideOffer offer = state.getCurrentOffer();
        offer.setStatus(OfferStatus.EXPIRED);
        offerRepository.save(offer);

        state.incrementIndex();
        sendNextOffer(state);
    }

    private void handleNoDriversAvailable(Ride ride) {
        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);
        activeFlows.remove(ride.getId());
        // TODO: Notify rider
    }
}
