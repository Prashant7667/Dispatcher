package org.dispatchsystem.ride.service;
import org.dispatchsystem.common.events.RideCancelledEvent;
import org.dispatchsystem.common.exceptions.BusinessRuleViolationException;
import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.dispatch.orchestrator.DispatchOrchestrator;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.domain.VehicleClass;
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
    private final FareEstimationService fareEstimationService;
    private final DispatchAuditService dispatchAuditService;

    public RideService(RideRepository rideRepository, DriverRepository driverRepository, UserService userService, DispatchOrchestrator dispatchOrchestrator, ApplicationEventPublisher applicationEventPublisher, RideStateMachine rideStateMachine,OfferManager offerManager, FareEstimationService fareEstimationService, DispatchAuditService dispatchAuditService){
        this.rideRepository=rideRepository;
        this.driverRepository=driverRepository;
        this.userService=userService;
        this.dispatchOrchestrator=dispatchOrchestrator;
        this.applicationEventPublisher=applicationEventPublisher;
        this.rideStateMachine=rideStateMachine;
        this.offerManager=offerManager;
        this.fareEstimationService = fareEstimationService;
        this.dispatchAuditService = dispatchAuditService;
    }

    public Ride requestRide(double startLongitude, double startLatitude, double endLongitude, double endLatitude,
            BookingType bookingType,
            LocalDateTime scheduledStart,
            Integer estimatedDurationMinutes,
            RentalPlan rentalPlan, VehicleClass requestedVehicleClass, int requiredLuggageCapacity) {
        var passenger=userService.getCurrentPassengerDetails();
        RentalPlan normalizedRentalPlan = rentalPlan == null ? RentalPlan.NONE : rentalPlan;
        double fare= fareEstimationService.fareEstimation(startLatitude, startLongitude, endLatitude, endLongitude, bookingType,estimatedDurationMinutes, normalizedRentalPlan);
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
        ride.setRentalPlan(normalizedRentalPlan);
        ride.setFare(fare);
        ride.setCreatedAt(LocalDateTime.now());
        if(scheduledStart!=null && scheduledStart.isAfter(LocalDateTime.now())){
            ride.setStatus(RideStatus.SCHEDULED);
        }
        else ride.setStatus(RideStatus.REQUESTED);
        ride.setRequestedVehicleClass(requestedVehicleClass);
        ride.setRequiredLuggageCapacity(requiredLuggageCapacity);
        Ride savedRide = rideRepository.save(ride);
        if(ride.getStatus()!=RideStatus.SCHEDULED)
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
        ride.setCancelledAt(LocalDateTime.now());

        rideStateMachine.transition(ride,RideStatus.CANCELLED);

        offerManager.cancelRideFlow(rideId);
        Driver driver=ride.getDriver();
        if(driver!=null){
            driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            driverRepository.save(driver);
        }
        dispatchAuditService.recordRideCancelled(ride, driver, null, org.dispatchsystem.common.events.domains.ReasonCode.PASSENGER_CANCELLED, "Ride cancelled by passenger");
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
        ride.setCompletedAt(LocalDateTime.now());
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
        if(!(existingRide.getStatus()==RideStatus.REQUESTED|| existingRide.getStatus()==RideStatus.SCHEDULED)){
            throw new BusinessRuleViolationException("We can't update the ride in this case");
        }
        if(existingRide.getStatus()==RideStatus.SCHEDULED){
            existingRide.setScheduledStart(updatedData.getScheduledStart());
        }
        existingRide.setStartLongitude(updatedData.getStartLongitude());
        existingRide.setStartLatitude(updatedData.getStartLatitude());
        existingRide.setEndLatitude(updatedData.getEndLatitude());
        existingRide.setEndLongitude(updatedData.getEndLongitude());
        existingRide.setBookingType(updatedData.getBookingType());
        existingRide.setScheduledStart(updatedData.getScheduledStart());
        existingRide.setEstimatedDurationMinutes(updatedData.getEstimatedDurationMinutes());
        RentalPlan normalizedRentalPlan = updatedData.getRentalPlan() == null ? RentalPlan.NONE : updatedData.getRentalPlan();
        existingRide.setRentalPlan(normalizedRentalPlan);
        existingRide.setRequestedVehicleClass(updatedData.getRequestedVehicleClass());
        existingRide.setRequiredLuggageCapacity(updatedData.getRequiredLuggageCapacity());
        double fare= fareEstimationService.fareEstimation(updatedData.getStartLatitude(), updatedData.getStartLongitude(), updatedData.getEndLatitude(), updatedData.getEndLongitude(),updatedData.getBookingType(),updatedData.getEstimatedDurationMinutes(), normalizedRentalPlan);
        existingRide.setFare(fare);
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
