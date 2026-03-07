package org.dispatchsystem.user.service;

import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.user.domain.User;
import org.dispatchsystem.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;

    UserService(UserRepository passengerRepository, PasswordEncoder passwordEncoder) {
        this.passengerRepository = passengerRepository;
         this.passwordEncoder=passwordEncoder;
    }
    public User registerUser(User passenger) {
        if (passenger.getPassword() != null && !passenger.getPassword().isBlank()) {
            passenger.setPassword(passwordEncoder.encode(passenger.getPassword()));
        }
        return passengerRepository.save(passenger);
    }

    public User getCurrentPassengerDetails() {
         Authentication auth= SecurityContextHolder.getContext().getAuthentication();
         return passengerRepository.findByEmail(auth.getName())
         .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
    }

    public List<User> getAllPassengers() {
        return passengerRepository.findAll();
    }

    public User updatePassenger(User updatedData) {
        User existingPassenger = getCurrentPassengerDetails();
        if (updatedData.getName() != null) {
            existingPassenger.setName(updatedData.getName());
        }
        if (updatedData.getPassword() != null && !updatedData.getPassword().isBlank()) {
            existingPassenger.setPassword(passwordEncoder.encode(updatedData.getPassword()));
        }
        if (updatedData.getPhoneNumber() != null) {
            existingPassenger.setPhoneNumber(updatedData.getPhoneNumber());
        }

        return passengerRepository.save(existingPassenger);
    }

    public void deletePassenger() {
        User passenger = getCurrentPassengerDetails();
        passengerRepository.delete(passenger);
    }
}
