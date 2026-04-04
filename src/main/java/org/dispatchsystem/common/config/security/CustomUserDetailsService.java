package org.dispatchsystem.common.config.security;

import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.user.domain.User;
import org.dispatchsystem.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public CustomUserDetailsService(DriverRepository driverRepository, UserRepository userRepository) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Driver driver = driverRepository.findByEmail(email).orElse(null);
        if (driver != null) {
            return new UserDetailsImpl(driver.getEmail(), driver.getPassword(), "DRIVER");
        }

        User passenger = userRepository.findByEmail(email).orElse(null);
        if (passenger != null) {
            return new UserDetailsImpl(passenger.getEmail(), passenger.getPassword(), "USER");
        }

        throw new UsernameNotFoundException("No driver or passenger found with email: " + email);
    }
}
