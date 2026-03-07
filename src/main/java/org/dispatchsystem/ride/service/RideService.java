package org.dispatchsystem.ride.service;
import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.dispatch.orchestrator.DispatchOrchestrator;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.driver.service.DriverService;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.user.repository.UserRepository;
import org.dispatchsystem.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class RideService {
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverService driverService;
    private final UserService userService;
    private final DispatchOrchestrator dispatchOrchestrator;

    public RideService(RideRepository rideRepository, DriverRepository driverRepository, UserRepository userRepository, DriverService driverService, UserService userService, DispatchOrchestrator dispatchOrchestrator){
        this.rideRepository=rideRepository;
        this.driverRepository=driverRepository;
        this.userRepository=userRepository;
        this.driverService=driverService;
        this.userService=userService;
        this.dispatchOrchestrator=dispatchOrchestrator;
    }

    public Ride requestRide(double startLongitude, double startLatitude, double endLongitude, double endLatitude,
            Double fare) {
        var passenger=userService.getCurrentPassengerDetails();
        Ride ride = new Ride();
        ride.setUser(passenger);
        ride.setDriver(null);
        ride.setStartLongitude(startLongitude);
        ride.setStartLatitude(startLatitude);
        ride.setEndLongitude(endLongitude);
        ride.setEndLatitude(endLatitude);
        ride.setFare(fare);
        ride.setStatus(RideStatus.REQUESTED);
        Ride savedRide = rideRepository.save(ride);
        dispatchOrchestrator.dispatch(ride);
        return savedRide;
    }

    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    public Ride getRideById(Long id) throws ResourceNotFoundException {
        return rideRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Ride not found with id: " + id));

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
}
