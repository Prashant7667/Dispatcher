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
                req.getBookingType(),
                req.getScheduledStart(),
                req.getEstimatedDurationMinutes(),
                req.getRentalPlan(),
                req.getRequestedVehicleClass(),
                req.getRequiredLuggageCapacity()
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
        updatedEntity.setBookingType(ride.getBookingType());
        updatedEntity.setScheduledStart(ride.getScheduledStart());
        updatedEntity.setEstimatedDurationMinutes(ride.getEstimatedDurationMinutes());
        updatedEntity.setRentalPlan(ride.getRentalPlan());
        updatedEntity.setRequestedVehicleClass(ride.getRequestedVehicleClass());
        updatedEntity.setRequiredLuggageCapacity(ride.getRequiredLuggageCapacity());
        Ride savedRide = rideService.updateRide(id, updatedEntity);
        return ResponseEntity.ok(toResponseDto(savedRide));
    }
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/me/{rideId}/en-route")
    public ResponseEntity<RideResponseDTO> markDriverEnRoute(@PathVariable Long rideId) {
        Ride updatedRide = rideService.markDriverEnRoute(rideId);
        return ResponseEntity.ok(toResponseDto(updatedRide));
    }
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/me/{rideId}/arrived")
    public ResponseEntity<RideResponseDTO> markDriverArrived(@PathVariable Long rideId) {
        Ride updatedRide = rideService.markDriverArrived(rideId);
        return ResponseEntity.ok(toResponseDto(updatedRide));
    }
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/me/{rideId}/start")
    public ResponseEntity<RideResponseDTO> startRide(@PathVariable Long rideId) {
        Ride updatedRide = rideService.startRide(rideId);
        return ResponseEntity.ok(toResponseDto(updatedRide));
    }
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/me/{rideId}/complete")
    public ResponseEntity<RideResponseDTO> completeRide(@PathVariable Long rideId) {
        Ride updatedRide = rideService.completeRide(rideId);
        return ResponseEntity.ok(toResponseDto(updatedRide));
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
        responseDTO.setBookingType(ride.getBookingType());
        responseDTO.setScheduledStart(ride.getScheduledStart());
        responseDTO.setEstimatedDurationMinutes(ride.getEstimatedDurationMinutes());
        responseDTO.setRentalPlan(ride.getRentalPlan());
        responseDTO.setStatus(ride.getStatus());
        responseDTO.setFare(ride.getFare());
        responseDTO.setRequestedVehicleClass(ride.getRequestedVehicleClass());
        responseDTO.setRequiredLuggageCapacity(ride.getRequiredLuggageCapacity());
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
