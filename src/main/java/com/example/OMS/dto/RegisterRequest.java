package com.example.OMS.dto;

import com.example.OMS.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // CUSTOMER or SELLER; null defaults to CUSTOMER. ADMIN cannot be self-registered.
    private Role role;
}
