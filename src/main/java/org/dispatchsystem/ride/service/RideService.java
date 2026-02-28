package org.dispatchsystem.ride.service;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class RideService {
    @Autowired
    public static RideRepository rideRepository;
    @Autowired
    public static DriverRepository driverRepository;
    public Ride requestRide(double  startLongitude, double startLatitude, double  endLongitude, double endLatitude, Double fare){
        User passenger=new User();
        Ride ride=new Ride();
        ride.setUser(passenger);
        ride.setDriver(null);
        ride.setStartLongitude(startLongitude);
        ride.setStartLatitude(startLatitude);
        ride.setEndLongitude(endLongitude);
        ride.setEndLatitude(endLatitude);
        ride.setFare(fare);
        ride.setStatus(Ride.RideStatus.REQUESTED);
        Ride savedRide= rideRepository.save(ride);
        return savedRide;
    }
    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    public Ride getRideById(Long id) {
        return rideRepository.findById(id).orElseThrow();

    }
    public List<Ride> getPassengerRideHistory() {
       String email = "pkp@gmail.com";
       return rideRepository.findByPassengerEmail(email);
    }
    public List<Ride> getDriverRideHistory() {
        String email = "pkp@gmail.com";
        return rideRepository.findByDriverEmail(email);
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
        if (driver != null && driver.getAvailabilityStatus() == Driver.AvailabilityStatus.UNAVAILABLE) {
            driver.setAvailabilityStatus(Driver.AvailabilityStatus.AVAILABLE);
            driverRepository.save(driver);
        }
        rideRepository.delete(ride);
    }
}
