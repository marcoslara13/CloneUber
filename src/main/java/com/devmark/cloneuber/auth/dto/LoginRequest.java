package com.devmark.cloneuber.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequest {
    @Email
    private String email;
    @NotBlank
    private String password;
}
