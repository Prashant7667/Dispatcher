package org.dispatchsystem.user.controller;

import jakarta.validation.Valid;
import org.dispatchsystem.common.config.security.JwtUtils;
import org.dispatchsystem.common.config.security.UserDetailsImpl;
import org.dispatchsystem.user.domain.User;
import org.dispatchsystem.user.dto.LoginRequest;
import org.dispatchsystem.user.dto.UserRequestDTO;
import org.dispatchsystem.user.dto.UserResponseDTO;
import org.dispatchsystem.user.dto.UserUpdateDTO;
import org.dispatchsystem.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService passengerService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    UserController(UserService passengerService, AuthenticationManager authenticationManager, JwtUtils jwtUtils){
        this.passengerService=passengerService;
        this.authenticationManager=authenticationManager;
        this.jwtUtils=jwtUtils;
    }
    @PostMapping
    public ResponseEntity<UserResponseDTO>RegisterUser(@Valid @RequestBody UserRequestDTO userRequest){
        User user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setPassword(userRequest.getPassword());
        user.setPhoneNumber(userRequest.getPhoneNumber());
        User savedUser = passengerService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(savedUser));
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
    public ResponseEntity<List<UserResponseDTO>> getAllPassengers() {
        List<User>savedPassengers= passengerService.getAllPassengers();
        return ResponseEntity.ok(savedPassengers.stream().map(this::toResponseDto).collect(Collectors.toList()));

    }
    @GetMapping("/me/details")
    public ResponseEntity<UserResponseDTO> getCurrentPassengerDetails() {
        User passenger = passengerService.getCurrentPassengerDetails();
        return ResponseEntity.ok(toResponseDto(passenger));
    }
    @PutMapping("/me/update")
    public ResponseEntity<UserResponseDTO> updatePassenger(@RequestBody UserUpdateDTO passenger) {
        User updatedPassenger = new User();
        updatedPassenger.setName(passenger.getName());
        updatedPassenger.setPassword(passenger.getPassword());
        updatedPassenger.setPhoneNumber(passenger.getPhoneNumber());
        User savedPassenger = passengerService.updatePassenger(updatedPassenger);
        return ResponseEntity.ok(toResponseDto(savedPassenger));
    }
    @DeleteMapping("/me/delete")
    public ResponseEntity<Void> deletePassenger() {
        passengerService.deletePassenger();
        return ResponseEntity.noContent().build();
    }

    private UserResponseDTO toResponseDto(User user) {
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(user.getId());
        responseDTO.setName(user.getName());
        responseDTO.setEmail(user.getEmail());
        responseDTO.setPhoneNumber(user.getPhoneNumber());
        return responseDTO;
    }
}
