package org.dispatchsystem.ride.controller;

import jakarta.validation.Valid;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public class RideController {
    @Autowired
    public static RideService rideService;
    public ResponseEntity<Ride> requestRide(@Valid @RequestBody Ride req){
        Ride requestedRide= rideService.requestRide(
                req.getStartLongitude(),
                req.getStartLatitude(),
                req.getEndLongitude(),
                req.getEndLatitude(),
                req.getFare()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(requestedRide);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Ride> getRideById(@PathVariable Long id) {
        Ride ride = rideService.getRideById(id);
        return ResponseEntity.ok(ride);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Ride> updateRide(@PathVariable Long id, @Valid @RequestBody Ride ride) {
        Ride updatedEntity = new Ride();
        updatedEntity.setStartLongitude(ride.getStartLongitude());
        updatedEntity.setStartLatitude(ride.getStartLatitude());
        updatedEntity.setEndLatitude(ride.getEndLatitude());
        updatedEntity.setEndLongitude(ride.getEndLongitude());
        if (ride.getStatus() != null) {
            updatedEntity.setStatus(Ride.RideStatus.valueOf(String.valueOf(ride.getStatus())));
        }

        updatedEntity.setFare(ride.getFare());

        Ride savedRide = rideService.updateRide(id, updatedEntity);
        return ResponseEntity.ok(savedRide);
    }
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/me/driver/rideHistory")
    public ResponseEntity<List<Ride>>rideDriverHistory(){
        List<Ride>rides= rideService.getDriverRideHistory();
        return ResponseEntity.ok(rides);
    }
}
