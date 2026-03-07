package org.dispatchsystem.driver.service;
import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    public DriverService(PasswordEncoder passwordEncoder,DriverRepository driverRepository){
        this.driverRepository=driverRepository;
        this.passwordEncoder=passwordEncoder;
    }

    public Driver createDriver(Driver driver) {
        if (driver.getPassword() != null && !driver.getPassword().isBlank()) {
             driver.setPassword(passwordEncoder.encode(driver.getPassword()));
        }
        return driverRepository.save(driver);
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getCurrentDriver() {
        Authentication auth= SecurityContextHolder.getContext().getAuthentication();
        return driverRepository.findByEmail(auth.getName()).orElseThrow(()->new ResourceNotFoundException("Driver Not Found"));
    }

    public Driver updateDriver(Driver updatedData) {
        Driver existingDriver = getCurrentDriver();

        if (updatedData.getName() != null)
            existingDriver.setName(updatedData.getName());

        if (updatedData.getPassword() != null && !updatedData.getPassword().isBlank())
            existingDriver.setPassword(passwordEncoder.encode(updatedData.getPassword()));

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
        Driver driver=getCurrentDriver();
        driverRepository.delete(driver);
    }

    public Driver updateDriverAvailability(AvailabilityStatus status) {
        Driver driver=getCurrentDriver();
        if(!driver.getAvailabilityStatus().equals(status)){
            driver.setAvailabilityStatus(status);
        }
        return driverRepository.save(driver);
    }

}
