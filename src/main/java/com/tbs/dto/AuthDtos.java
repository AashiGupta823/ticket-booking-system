package com.tbs.dto;

import com.tbs.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password,
            User.Role role
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role
    ) {}
}
