package org.dispatchsystem.ride.controller;

import jakarta.validation.Valid;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.dto.RideRequestDTO;
import org.dispatchsystem.ride.dto.RideResponseDTO;
import org.dispatchsystem.ride.dto.RideUpdateDTO;
import org.dispatchsystem.ride.service.RideService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("rides")
public class RideController {
    private final RideService rideService;
    RideController(RideService rideService){
        this.rideService=rideService;
    }
    @PostMapping("/request")
    public ResponseEntity<RideResponseDTO> requestRide(@Valid @RequestBody RideRequestDTO req){
        Ride requestedRide= rideService.requestRide(
                req.getStartLongitude(),
                req.getStartLatitude(),
                req.getEndLongitude(),
                req.getEndLatitude(),
                req.getFare()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(requestedRide));
    }
    @GetMapping("/{id}")
    public ResponseEntity<RideResponseDTO> getRideById(@PathVariable Long id) {
        Ride ride = rideService.getRideById(id);
        return ResponseEntity.ok(toResponseDto(ride));
    }
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/me/{rideId}/cancel")
    public ResponseEntity<RideResponseDTO> cancelRideByPassenger(@PathVariable Long rideId){
        Ride cancelRide=rideService.cancelRideByPassenger(rideId);
        return ResponseEntity.ok(toResponseDto(cancelRide));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RideResponseDTO> updateRide(@PathVariable Long id, @Valid @RequestBody RideUpdateDTO ride) {
        Ride updatedEntity = new Ride();
        updatedEntity.setStartLongitude(ride.getStartLongitude());
        updatedEntity.setStartLatitude(ride.getStartLatitude());
        updatedEntity.setEndLatitude(ride.getEndLatitude());
        updatedEntity.setEndLongitude(ride.getEndLongitude());
        updatedEntity.setFare(ride.getFare());
        Ride savedRide = rideService.updateRide(id, updatedEntity);
        return ResponseEntity.ok(toResponseDto(savedRide));
    }
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/me/driver/rideHistory")
    public ResponseEntity<List<RideResponseDTO>>rideDriverHistory(){
        List<Ride>rides= rideService.getDriverRideHistory();
        return ResponseEntity.ok(rides.stream().map(this::toResponseDto).collect(Collectors.toList()));
    }
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me/user/rideHistory")
    public ResponseEntity<List<RideResponseDTO>>ridePassengerHistory(){
        List<Ride>rides= rideService.getPassengerRideHistory();
        return ResponseEntity.ok(rides.stream().map(this::toResponseDto).collect(Collectors.toList()));
    }

    private RideResponseDTO toResponseDto(Ride ride) {
        RideResponseDTO responseDTO = new RideResponseDTO();
        responseDTO.setId(ride.getId());
        responseDTO.setStartLongitude(ride.getStartLongitude());
        responseDTO.setStartLatitude(ride.getStartLatitude());
        responseDTO.setEndLongitude(ride.getEndLongitude());
        responseDTO.setEndLatitude(ride.getEndLatitude());
        responseDTO.setStatus(ride.getStatus());
        responseDTO.setFare(ride.getFare());
        if (ride.getDriver() != null) {
            responseDTO.setDriverId(ride.getDriver().getId());
            responseDTO.setDriverName(ride.getDriver().getName());
        }
        if (ride.getUser() != null) {
            responseDTO.setUserId(ride.getUser().getId());
            responseDTO.setUserName(ride.getUser().getName());
        }
        return responseDTO;
    }
}
