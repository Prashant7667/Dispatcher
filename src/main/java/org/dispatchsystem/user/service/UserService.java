package org.dispatchsystem.user.service;

import org.dispatchsystem.user.domain.User;
import org.dispatchsystem.user.repository.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository passengerRepository;
    UserService(UserRepository passengerRepository){
        this.passengerRepository=passengerRepository;
        //this.passwordEncoder=passwordEncoder;
    }
    public User createPassenger(User passenger) {
        if(passenger.getPassword()!=null && !passenger.getPassword().isBlank()){
            passenger.setPassword(passenger.getPassword());
        }
        return passengerRepository.save(passenger);
    }
    public User getCurrentPassengerDetails() {
        return new User();
//        Authentication auth= SecurityContextHolder.getContext().getAuthentication();
//        return passengerRepository.findByEmail(auth.getName())
//                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
    }
    public List<User> getAllPassengers() {
        return passengerRepository.findAll();
    }
    public User updatePassenger(User updatedData) {
        User existingPassenger = getCurrentPassengerDetails();
        if(updatedData.getName()!=null) {
            existingPassenger.setName(updatedData.getName());
        }
        if(updatedData.getPassword()!=null && !updatedData.getPassword().isBlank()){
            existingPassenger.setPassword(updatedData.getPassword());
        }
        if(updatedData.getPhoneNumber()!=null){
            existingPassenger.setPhoneNumber(updatedData.getPhoneNumber());
        }

        return passengerRepository.save(existingPassenger);
    }
    public void deletePassenger() {
        User passenger = getCurrentPassengerDetails();
        passengerRepository.delete(passenger);
    }
}
