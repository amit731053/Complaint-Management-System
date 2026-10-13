package com.cms.cms.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 60, message = "Name too long")
        @Pattern(regexp = "^[^<>]*$", message = "Name contains invalid characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        @Size(max = 100, message = "Email too long")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password needs upper, lower case and a number")
        String password) {
}
