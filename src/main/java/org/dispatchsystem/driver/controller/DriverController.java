package org.dispatchsystem.driver.controller;
import jakarta.validation.Valid;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.dto.DriverRequestDTO;
import org.dispatchsystem.driver.dto.DriverResponseDTO;
import org.dispatchsystem.driver.dto.DriverUpdateDTO;
import org.dispatchsystem.driver.service.DriverService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/drivers")
public class DriverController {
    private DriverService driverService;
    DriverController(DriverService driverService){
        this.driverService = driverService;
    }
    @PostMapping
    public ResponseEntity<DriverResponseDTO> createDriver(@Valid @RequestBody DriverRequestDTO driver) {
        Driver driverEntity = new Driver();
        driverEntity.setName(driver.getName());
        driverEntity.setEmail(driver.getEmail());
        driverEntity.setPassword(driver.getPassword());
        driverEntity.setPhoneNumber(driver.getPhoneNumber());
        driverEntity.setVehicleDetails(driver.getVehicleDetails());
        driverEntity.setLongitude(driver.getLongitude());
        driverEntity.setLatitude(driver.getLatitude());
        driverEntity.setSupportedBookingTypes(driver.getSupportedBookingTypes());
        driverEntity.setAvailableFrom(driver.getAvailableFrom());
        driverEntity.setAvailableUntil(driver.getAvailableUntil());
        driverEntity.setMaxRentalDurationMinutes(driver.getMaxRentalDurationMinutes());
        Driver savedDriver=driverService.createDriver(driverEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(savedDriver));

    }
    @GetMapping
    public ResponseEntity<List<DriverResponseDTO>> getAllDrivers() {
        List<Driver>savedDrivers= driverService.getAllDrivers();
        return ResponseEntity.ok(savedDrivers.stream().map(this::toResponseDto).collect(Collectors.toList()));
    }
    @PutMapping("/me")
    public ResponseEntity<DriverResponseDTO> updateDriver(@Valid @RequestBody DriverUpdateDTO driver) {
        Driver driverEntity = new Driver();
        driverEntity.setName(driver.getName());
        driverEntity.setPassword(driver.getPassword());
        driverEntity.setPhoneNumber(driver.getPhoneNumber());
        driverEntity.setVehicleDetails(driver.getVehicleDetails());
        driverEntity.setLatitude(driver.getLatitude());
        driverEntity.setLongitude(driver.getLongitude());
        driverEntity.setSupportedBookingTypes(driver.getSupportedBookingTypes());
        driverEntity.setAvailableFrom(driver.getAvailableFrom());
        driverEntity.setAvailableUntil(driver.getAvailableUntil());
        driverEntity.setMaxRentalDurationMinutes(driver.getMaxRentalDurationMinutes());
        driverEntity.setAvailabilityStatus(driver.getAvailabilityStatus());
        Driver updatedDriver=  driverService.updateDriver(driverEntity);
        return ResponseEntity.ok(toResponseDto(updatedDriver));
    }
    @DeleteMapping
    public ResponseEntity<Void> deleteDriver() {
        driverService.deleteDriver();
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/me")
    public ResponseEntity<DriverResponseDTO> getCurrentDriver() {
        Driver currentDriver=driverService.getCurrentDriver();
        return ResponseEntity.ok(toResponseDto(currentDriver));
    }
    @PatchMapping("/me/availability")
    public ResponseEntity<DriverResponseDTO> updateDriverAvailability(@RequestParam String status) {
        AvailabilityStatus availabilityStatus = AvailabilityStatus.valueOf(status.toUpperCase());
        Driver updatedDriver= driverService.updateDriverAvailability(availabilityStatus);
        return ResponseEntity.ok(toResponseDto(updatedDriver));
    }

    private DriverResponseDTO toResponseDto(Driver driver) {
        DriverResponseDTO responseDTO = new DriverResponseDTO();
        responseDTO.setId(driver.getId());
        responseDTO.setName(driver.getName());
        responseDTO.setEmail(driver.getEmail());
        responseDTO.setPhoneNumber(driver.getPhoneNumber());
        responseDTO.setVehicleDetails(driver.getVehicleDetails());
        responseDTO.setLatitude(driver.getLatitude());
        responseDTO.setLongitude(driver.getLongitude());
        responseDTO.setAvgRating(driver.getAvgRating());
        responseDTO.setTotalRating(driver.getTotalRating());
        responseDTO.setSupportedBookingTypes(driver.getSupportedBookingTypes());
        responseDTO.setAvailableFrom(driver.getAvailableFrom());
        responseDTO.setAvailableUntil(driver.getAvailableUntil());
        responseDTO.setMaxRentalDurationMinutes(driver.getMaxRentalDurationMinutes());
        responseDTO.setAvailabilityStatus(driver.getAvailabilityStatus());
        return responseDTO;
    }
}
