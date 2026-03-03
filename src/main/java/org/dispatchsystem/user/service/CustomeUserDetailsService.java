package org.dispatchsystem.user.service;

import org.dispatchsystem.common.config.UserDetailsImpl;
import org.dispatchsystem.user.domain.User;
import org.dispatchsystem.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomeUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    public CustomeUserDetailsService(UserRepository userRepository){
        this.userRepository=userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String email)throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User Not found with this email"));
        return new UserDetailsImpl(user.getEmail(), user.getPassword(), "User");

    }
}
