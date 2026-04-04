package org.dispatchsystem.dispatch.orchestrator;
import org.dispatchsystem.dispatch.DispatchDecision;
import org.dispatchsystem.dispatch.DispatchDecisionService;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.ride.service.RideStateMachine;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DispatchOrchestrator {
    private final OfferManager offerManager;
    private final RideRepository rideRepository;
    private final RideStateMachine rideStateMachine;
    private final DriverRepository driverRepository;
    private final DispatchDecisionService dispatchDecisionService;
    DispatchOrchestrator(OfferManager offerManager, RideRepository rideRepository, RideStateMachine rideStateMachine, DriverRepository driverRepository, DispatchDecisionService dispatchDecisionService){
        this.offerManager = offerManager;
        this.rideRepository = rideRepository;
        this.rideStateMachine = rideStateMachine;
        this.driverRepository = driverRepository;
        this.dispatchDecisionService = dispatchDecisionService;
    }
    public void dispatch(Ride ride){
        List<Driver> availableDrivers= driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        DispatchDecision decision= dispatchDecisionService.takeDecision(ride, availableDrivers);
        if(!decision.hasEligibleCandidates()){
            rideStateMachine.transition(ride, RideStatus.CANCELLED);
            rideRepository.save(ride);
            // TODO: notify rider "no drivers available"
            return;
        }
        // Step 3: Start the offer flow — try drivers one by one
        rideStateMachine.transition(ride, RideStatus.DISPATCHING);
        rideRepository.save(ride);
        offerManager.startOfferFlow(ride, decision.rankedDrivers());
    }
}
