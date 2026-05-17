package com.devmark.cloneuber.auth.dto;

import com.devmark.cloneuber.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterRequest {
    @NotBlank
    private String name;
    @Email
    private String email;
    @NotBlank @Size(min = 6) private String password;
    private Role role;
}