package org.dispatchsystem.driver.service;

import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {
    @Autowired
    private DriverRepository driverRepository;

    public Driver createDriver(Driver driver) {
        if (driver.getPassword() != null && !driver.getPassword().isBlank()) {
            // driver.setPassword(passwordEncoder.encode(driver.getPassword()));
        }
        return driverRepository.save(driver);
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getCurrentDriver() {
        return new Driver();
    }

    public Driver updateDriver(Driver updatedData) {
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // String email= ;
        Driver existingDriver = updatedData;

        if (updatedData.getName() != null)
            existingDriver.setName(updatedData.getName());

        if (updatedData.getPassword() != null && !updatedData.getPassword().isBlank())
            existingDriver.setPassword(updatedData.getPassword());

        if (updatedData.getPhoneNumber() != null)
            existingDriver.setPhoneNumber(updatedData.getPhoneNumber());

        if (updatedData.getVehicleDetails() != null)
            existingDriver.setVehicleDetails(updatedData.getVehicleDetails());

        if (updatedData.getAvailabilityStatus() != null)
            existingDriver.setAvailabilityStatus(updatedData.getAvailabilityStatus());

        if (updatedData.getLatitude() != null)
            existingDriver.setLatitude(updatedData.getLatitude());

        if (updatedData.getLongitude() != null)
            existingDriver.setLongitude(updatedData.getLongitude());

        return driverRepository.save(existingDriver);
    }

    public void deleteDriver() {
    }

    public Driver updateDriverAvailability(Driver.AvailabilityStatus status) {
        return new Driver();
    }

}
