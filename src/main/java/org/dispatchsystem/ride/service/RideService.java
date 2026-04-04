package org.dispatchsystem.ride.service;
import org.dispatchsystem.common.events.RideCancelledEvent;
import org.dispatchsystem.common.exceptions.BusinessRuleViolationException;
import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.dispatch.orchestrator.DispatchOrchestrator;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.RentalPlan;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class RideService {
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final UserService userService;
    private final DispatchOrchestrator dispatchOrchestrator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RideStateMachine rideStateMachine;
    private final OfferManager offerManager;

    public RideService(RideRepository rideRepository, DriverRepository driverRepository, UserService userService, DispatchOrchestrator dispatchOrchestrator, ApplicationEventPublisher applicationEventPublisher, RideStateMachine rideStateMachine,OfferManager offerManager){
        this.rideRepository=rideRepository;
        this.driverRepository=driverRepository;
        this.userService=userService;
        this.dispatchOrchestrator=dispatchOrchestrator;
        this.applicationEventPublisher=applicationEventPublisher;
        this.rideStateMachine=rideStateMachine;
        this.offerManager=offerManager;
    }

    public Ride requestRide(double startLongitude, double startLatitude, double endLongitude, double endLatitude,
            BookingType bookingType,
            LocalDateTime scheduledStart,
            Integer estimatedDurationMinutes,
            RentalPlan rentalPlan,
            Double fare) {
        var passenger=userService.getCurrentPassengerDetails();
        Ride ride = new Ride();
        ride.setUser(passenger);
        ride.setDriver(null);
        ride.setStartLongitude(startLongitude);
        ride.setStartLatitude(startLatitude);
        ride.setEndLongitude(endLongitude);
        ride.setEndLatitude(endLatitude);
        ride.setBookingType(bookingType);
        ride.setScheduledStart(scheduledStart);
        ride.setEstimatedDurationMinutes(estimatedDurationMinutes);
        ride.setRentalPlan(rentalPlan == null ? RentalPlan.NONE : rentalPlan);
        ride.setFare(fare);
        ride.setStatus(RideStatus.REQUESTED);
        Ride savedRide = rideRepository.save(ride);
        dispatchOrchestrator.dispatch(savedRide);
        return savedRide;
    }

    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    public Ride getRideById(Long id) throws ResourceNotFoundException {
        return rideRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Ride not found with id: " + id));

    }
    public Ride cancelRideByPassenger(Long rideId){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        Ride ride=getRideById(rideId);
        if(!ride.getUser().getEmail().equals(auth.getName())){
            throw new BusinessRuleViolationException("You are not authorised to process this request");
        }
        if(ride.getStatus()== RideStatus.COMPLETED){
            throw new BusinessRuleViolationException("Ride Is Already Completed");
        }

        rideStateMachine.transition(ride,RideStatus.CANCELLED);

        offerManager.cancelRideFlow(rideId);
        Driver driver=ride.getDriver();
        if(driver!=null){
            driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            driverRepository.save(driver);
        }
        applicationEventPublisher.publishEvent(new RideCancelledEvent(ride));
        return rideRepository.save(ride);
    }

    public Ride markDriverEnRoute(Long rideId) {
        Ride ride = getRideForCurrentDriver(rideId);
        rideStateMachine.transition(ride, RideStatus.DRIVER_EN_ROUTE);
        return rideRepository.save(ride);
    }

    public Ride markDriverArrived(Long rideId) {
        Ride ride = getRideForCurrentDriver(rideId);
        rideStateMachine.transition(ride, RideStatus.DRIVER_ARRIVED);
        return rideRepository.save(ride);
    }

    public Ride startRide(Long rideId) {
        Ride ride = getRideForCurrentDriver(rideId);
        rideStateMachine.transition(ride, RideStatus.IN_PROGRESS);
        return rideRepository.save(ride);
    }

    public Ride completeRide(Long rideId) {
        Ride ride = getRideForCurrentDriver(rideId);
        rideStateMachine.transition(ride, RideStatus.COMPLETED);

        Driver driver = ride.getDriver();
        if (driver != null && driver.getAvailabilityStatus() == AvailabilityStatus.UNAVAILABLE) {
            driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            driverRepository.save(driver);
        }

        return rideRepository.save(ride);
    }

    public List<Ride> getPassengerRideHistory() {
       Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        return rideRepository.findByUserEmail(auth.getName());
    }
    public List<Ride> getDriverRideHistory() {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        return rideRepository.findByDriverEmail(auth.getName());
    }
    public Ride updateRide(Long id, Ride updatedData) {
        Ride existingRide = getRideById(id);
        existingRide.setStartLongitude(updatedData.getStartLongitude());
        existingRide.setStartLatitude(updatedData.getStartLatitude());
        existingRide.setEndLatitude(updatedData.getEndLatitude());
        existingRide.setEndLongitude(updatedData.getEndLongitude());
        existingRide.setBookingType(updatedData.getBookingType());
        existingRide.setScheduledStart(updatedData.getScheduledStart());
        existingRide.setEstimatedDurationMinutes(updatedData.getEstimatedDurationMinutes());
        existingRide.setRentalPlan(updatedData.getRentalPlan());
        existingRide.setFare(updatedData.getFare());
        return rideRepository.save(existingRide);
    }
    public void deleteRide(Long id) {
        Ride ride = getRideById(id);
        Driver driver = ride.getDriver();
        if (driver != null && driver.getAvailabilityStatus() == AvailabilityStatus.UNAVAILABLE) {
            driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            driverRepository.save(driver);
        }
        rideRepository.delete(ride);
    }

    private Ride getRideForCurrentDriver(Long rideId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Ride ride = getRideById(rideId);

        if (ride.getDriver() == null) {
            throw new BusinessRuleViolationException("No driver is assigned to this ride");
        }

        if (!ride.getDriver().getEmail().equals(auth.getName())) {
            throw new BusinessRuleViolationException("You are not authorised to process this ride");
        }

        return ride;
    }
}
