package org.dispatchsystem.dispatch.offer;

import org.dispatchsystem.common.events.DriverAssignedEvent;
import org.dispatchsystem.common.events.DriverOfferCreatedEvent;
import org.dispatchsystem.common.events.NoDriversAvailableEvent;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.common.exceptions.BusinessRuleViolationException;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.OfferStatus;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideOffer;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideOfferRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.dispatchsystem.ride.service.RideStateMachine;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Service
public class OfferManager {

    private final RideOfferRepository offerRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RideStateMachine rideStateMachine;
    private final DispatchAuditService dispatchAuditService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    OfferManager(RideOfferRepository offerRepository, RideRepository rideRepository, DriverRepository driverRepository, ApplicationEventPublisher applicationEventPublisher, RideStateMachine rideStateMachine, DispatchAuditService dispatchAuditService){
        this.offerRepository = offerRepository;
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.rideStateMachine = rideStateMachine;
        this.dispatchAuditService = dispatchAuditService;
    }
    // Tracks which ride is on which offer attempt
    // Key: rideId → Value: the current state of the offer flow
    private final ConcurrentHashMap<Long, OfferFlowState> activeFlows = new ConcurrentHashMap<>();
    public void cancelRideFlow(Long rideId){
        OfferFlowState state=activeFlows.remove(rideId);
        if(state==null)return;
        ScheduledFuture<?>timeoutFuture = state.getTimeoutFuture();
        if(timeoutFuture!=null){
            timeoutFuture.cancel(false);
        }
        RideOffer currentOffer=state.getCurrentOffer();
        if(currentOffer!=null && currentOffer.getStatus()==OfferStatus.PENDING){
            currentOffer.setStatus(OfferStatus.CANCELLED);
            currentOffer.setRespondedAt(LocalDateTime.now());
            Driver driver=currentOffer.getDriver();
            if(currentOffer.getDriver()!=null){

                driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
                driverRepository.save(driver);
                currentOffer.setDriver(driver);
            }
            offerRepository.save(currentOffer);
            dispatchAuditService.recordRideCancelled(
                    currentOffer.getRide(),
                    currentOffer.getDriver(),
                    state.getCurrentIndex() + 1,
                    ReasonCode.PASSENGER_CANCELLED,
                    "Ride was cancelled while an offer was pending"
            );
        }
    }
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

    @Transactional
    public void sendNextOffer(OfferFlowState state) {
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
        int val=driverRepository.updateDriver(nextDriver.getId(), AvailabilityStatus.AVAILABLE, AvailabilityStatus.RESERVED);
        if(val==0){
            movingToNextCandidate(state);
            return;
        }
        dispatchAuditService.recordOfferSent(state.getRide(), nextDriver, state.getCurrentIndex() + 1);
        applicationEventPublisher.publishEvent(new DriverOfferCreatedEvent(nextDriver,state.getRide()));
       // broadcastService.sendOfferToDriver(nextDriver, state.getRide());
        // 3. Schedule timeout — if no response in 30s, move to next
        ScheduledFuture<?> timeout = scheduler.schedule(
                () -> handleOfferTimeout(state),
                30, TimeUnit.SECONDS
        );
        state.setTimeoutFuture(timeout);
    }

    public DriverResponded resolveInactiveOfferResponse(Long rideId, String driverEmail) {
        return offerRepository.findTopByDriver_EmailAndRide_IdOrderBySentAtDesc(driverEmail, rideId)
                .map(offer -> {
                    if (offer.getStatus() == OfferStatus.EXPIRED) {
                        return new DriverResponded(OfferStatusState.OFFER_EXPIRED, rideId, "Offer already expired");
                    }
                    if (offer.getStatus() == OfferStatus.CANCELLED) {
                        return new DriverResponded(OfferStatusState.RIDE_CANCELLED, rideId, "Ride was cancelled");
                    }
                    return new DriverResponded(OfferStatusState.NO_ACTIVE_OFFER, rideId, "No active offer for this ride");
                })
                .orElseGet(() -> new DriverResponded(
                        OfferStatusState.NO_ACTIVE_OFFER,
                        rideId,
                        "No active offer for this ride"
                ));
    }

    public DriverResponded handleDriverResponse(Long rideId, String driverEmail, String response) {
        OfferFlowState state = activeFlows.get(rideId);

        if (state == null) {
            return resolveInactiveOfferResponse(rideId,driverEmail);
        }

        RideOffer currentOffer = state.getCurrentOffer();
        if (currentOffer == null || currentOffer.getDriver() == null) {
            return new DriverResponded(OfferStatusState.NO_ACTIVE_OFFER,rideId,"No active offer for this ride");
        }

        String offeredDriverEmail = currentOffer.getDriver().getEmail();
        if (offeredDriverEmail == null || !offeredDriverEmail.equalsIgnoreCase(driverEmail)) {
            return resolveInactiveOfferResponse(rideId,driverEmail);
        }

        // Cancel the timeout since driver responded
        if (state.getTimeoutFuture() != null) {
            state.getTimeoutFuture().cancel(false);
        }

        if ("ACCEPT".equals(response)) {
            // ✅ Driver accepted!
            RideOffer offer = currentOffer;
            offer.getDriver().setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
            offer.setStatus(OfferStatus.ACCEPTED);
            offer.setRespondedAt(LocalDateTime.now());
            offerRepository.save(offer);

            // Assign driver to ride
            Ride ride = state.getRide();
            ride.setDriver(offer.getDriver());
            ride.setDriverAssignedAt(LocalDateTime.now());
            rideStateMachine.transition(ride, RideStatus.DRIVER_ASSIGNED);
            rideRepository.save(ride);

            // Mark driver as unavailable
            Driver driver = offer.getDriver();
            driverRepository.save(driver);

            // Clean up
            activeFlows.remove(rideId);
            dispatchAuditService.recordOfferAccepted(ride, driver, state.getCurrentIndex() + 1);
            dispatchAuditService.recordDriverAssigned(ride, driver, state.getCurrentIndex() + 1);
            applicationEventPublisher.publishEvent(new DriverAssignedEvent(ride));
            return new DriverResponded(OfferStatusState.SUCCESS,rideId,"Ride is Accepted by the driver");

            // TODO: Notify rider that driver was found

        } else if ("REJECT".equals(response)) {
            // ❌ Driver rejected — try next
            RideOffer offer = currentOffer;
            offer.setStatus(OfferStatus.REJECTED);
            offer.getDriver().setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            driverRepository.save(offer.getDriver());
            offer.setRespondedAt(LocalDateTime.now());
            offerRepository.save(offer);
            dispatchAuditService.recordOfferRejected(state.getRide(), offer.getDriver(), state.getCurrentIndex() + 1);
            state.incrementIndex();
            sendNextOffer(state);
            return new DriverResponded(OfferStatusState.REJECTED,rideId,"Ride is Rejected by the driver");

        } else {
            throw new BusinessRuleViolationException("Unsupported driver response: " + response);
        }
    }

    /**
     * Called when the 30-second timeout fires.
     */
    public void handleOfferTimeout(OfferFlowState state) {
        RideOffer offer = state.getCurrentOffer();
        if(activeFlows.get(offer.getRide().getId())!=state){
            return;
        }
        offer.setStatus(OfferStatus.EXPIRED);
        Driver driver=offer.getDriver();
        driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        driverRepository.save(driver);
        offer.setDriver(driver);
        offer.setRespondedAt(LocalDateTime.now());
        offerRepository.save(offer);
        dispatchAuditService.recordOfferTimedOut(state.getRide(), driver, state.getCurrentIndex() + 1);

        state.incrementIndex();
        sendNextOffer(state);
    }
    public void movingToNextCandidate(OfferFlowState state) {
        RideOffer offer = state.getCurrentOffer();
        if(activeFlows.get(offer.getRide().getId())!=state){
            return;
        }
        offer.setStatus(OfferStatus.EXPIRED);
        offer.setRespondedAt(LocalDateTime.now());
        offerRepository.save(offer);
        dispatchAuditService.recordDriverSkipped(
                state.getRide(),
                offer.getDriver(),
                state.getCurrentIndex() + 1,
                ReasonCode.DRIVER_ALREADY_RESERVED,
                "Driver was no longer reservable when the offer flow attempted to contact them"
        );
        state.incrementIndex();
        sendNextOffer(state);
    }

    public void handleNoDriversAvailable(Ride ride) {
        ride.setCancelledAt(LocalDateTime.now());
        rideStateMachine.transition(ride, RideStatus.CANCELLED);
        rideRepository.save(ride);
        activeFlows.remove(ride.getId());
        dispatchAuditService.recordDispatchFailed(ride, ReasonCode.NO_ELIGIBLE_DRIVERS, "Dispatch exhausted all eligible drivers without an assignment");
        applicationEventPublisher.publishEvent(new NoDriversAvailableEvent(ride));
        // TODO: Notify rider
    }
}
