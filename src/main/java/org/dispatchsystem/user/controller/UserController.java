package org.dispatchsystem.user.controller;

import org.dispatchsystem.user.domain.User;
import org.dispatchsystem.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService passengerService;
    UserController(UserService passengerService){
        this.passengerService=passengerService;
    }
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllPassengers() {
        List<User>savedPassengers= passengerService.getAllPassengers();
        return ResponseEntity.ok(savedPassengers);

    }
    @GetMapping("/me/details")
    public ResponseEntity<User> getCurrentPassengerDetails() {
        User passenger = passengerService.getCurrentPassengerDetails();
        return ResponseEntity.ok(passenger);
    }
    @PutMapping("/me/update")
    public ResponseEntity<User> updatePassenger( @RequestBody User passenger) {
        User savedPassenger = passengerService.updatePassenger(passenger);
        return ResponseEntity.ok(savedPassenger);
    }
    @DeleteMapping("/me/delete")
    public ResponseEntity<Void> deletePassenger() {
        passengerService.deletePassenger();
        return ResponseEntity.noContent().build();
    }
}
