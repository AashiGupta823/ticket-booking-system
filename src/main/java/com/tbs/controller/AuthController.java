package com.tbs.controller;

import com.tbs.dto.AuthDtos.*;
import com.tbs.entity.User;
import com.tbs.repository.UserRepository;
import com.tbs.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User.Role role = req.role() == null ? User.Role.CUSTOMER : req.role();
        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name()));
    }
}
