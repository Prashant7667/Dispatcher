package org.dispatchsystem.ride.controller;

import jakarta.validation.Valid;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.service.RideService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("rides")
public class RideController {
    private final RideService rideService;
    RideController(RideService rideService){
        this.rideService=rideService;
    }
    @PostMapping("/request")
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
    public ResponseEntity<Ride> updateRide(@PathVariable Long id, @Valid @RequestBody Ride ride) throws IllegalAccessException {
        Ride updatedEntity = new Ride();
        updatedEntity.setStartLongitude(ride.getStartLongitude());
        updatedEntity.setStartLatitude(ride.getStartLatitude());
        updatedEntity.setEndLatitude(ride.getEndLatitude());
        updatedEntity.setEndLongitude(ride.getEndLongitude());
        updatedEntity.setFare(ride.getFare());
        if(ride.getStatus()!=updatedEntity.getStatus()){
            throw new IllegalStateException("We can't change the status");
        }
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
