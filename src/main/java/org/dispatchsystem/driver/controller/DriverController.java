package org.dispatchsystem.driver.controller;

import jakarta.validation.Valid;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("Drivers")
public class DriverController {
    @Autowired
    private DriverService driverService;
    @PostMapping
    public ResponseEntity<Driver> createDriver(@Valid @RequestBody Driver driver) {
        Driver driverEntity = new Driver();
        driverEntity.setName(driver.getName());
        driverEntity.setEmail(driver.getEmail());
        driverEntity.setPassword(driver.getPassword());
        driverEntity.setPhoneNumber(driver.getPhoneNumber());
        driverEntity.setVehicleDetails(driver.getVehicleDetails());
        driverEntity.setLongitude(driver.getLongitude());
        driverEntity.setLatitude(driver.getLatitude());
        if (driver.getAvailabilityStatus() != null) {
            driverEntity.setAvailabilityStatus(driver.getAvailabilityStatus());
        }
        Driver savedDriver=driverService.createDriver(driverEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDriver);

    }
    public ResponseEntity<List<Driver>> getAllDrivers() {
        List<Driver>savedDrivers= driverService.getAllDrivers();
        return ResponseEntity.ok(savedDrivers);
    }
    public ResponseEntity<Driver> updateDriver(@RequestBody Driver driver) {
        Driver updatedDriver=  driverService.updateDriver(driver);
        return ResponseEntity.ok(updatedDriver);
    }
    public ResponseEntity<Void> deleteDriver() {
        driverService.deleteDriver();
        return ResponseEntity.noContent().build();
    }
    public ResponseEntity<Driver> getCurrentDriver() {
        Driver currentDriver=driverService.getCurrentDriver();
        return ResponseEntity.ok(currentDriver);
    }
    public ResponseEntity<Driver> updateDriverAvailability(@RequestParam String status) {
        Driver.AvailabilityStatus availabilityStatus = Driver.AvailabilityStatus.valueOf(status.toUpperCase());
        Driver updatedDriver= driverService.updateDriverAvailability(availabilityStatus);
        return ResponseEntity.ok(updatedDriver);
    }

}
