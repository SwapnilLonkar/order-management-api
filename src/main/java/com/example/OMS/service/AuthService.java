package com.example.OMS.service;

import com.example.OMS.dto.AuthResponse;
import com.example.OMS.dto.LoginRequest;
import com.example.OMS.dto.RegisterRequest;
import com.example.OMS.entity.Role;
import com.example.OMS.entity.User;
import com.example.OMS.repository.UserRepository;
import com.example.OMS.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalStateException("Username already taken: " + request.getUsername());
        }
        Role role = request.getRole() == null ? Role.CUSTOMER : request.getRole();
        if (role == Role.ADMIN) {
            throw new IllegalStateException("Cannot self-register as ADMIN");
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user.getUsername(), user.getRole()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found: " + request.getUsername()));
        return new AuthResponse(jwtService.generateToken(user.getUsername(), user.getRole()));
    }
}
