package org.dispatchsystem.user.controller;

import jakarta.validation.Valid;
import org.dispatchsystem.common.config.JwtUtils;
import org.dispatchsystem.common.config.UserDetailsImpl;
import org.dispatchsystem.user.domain.LoginRequest;
import org.dispatchsystem.user.domain.User;
import org.dispatchsystem.user.repository.UserRepository;
import org.dispatchsystem.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService passengerService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    UserController(UserService passengerService,UserRepository userRepository,AuthenticationManager authenticationManager,JwtUtils jwtUtils){
        this.passengerService=passengerService;
        this.userRepository=userRepository;
        this.authenticationManager=authenticationManager;
        this.jwtUtils=jwtUtils;
    }
    @PostMapping
    public ResponseEntity<User>RegisterUser(@Valid @RequestBody User user){
        passengerService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    @PostMapping("/login")
    public ResponseEntity<?>loginRequest(@Valid @RequestBody LoginRequest loginRequest){
        var authToken=new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword());
        Authentication authentication=authenticationManager.authenticate(authToken);
        UserDetailsImpl userDetails=(UserDetailsImpl) authentication.getPrincipal();
        String jwt=jwtUtils.generateToken(userDetails.getUsername(),userDetails.getRole());
        return ResponseEntity.ok(Map.of("token",jwt,"role",userDetails.getRole()));
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
