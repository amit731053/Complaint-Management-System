package com.cms.cms.dto;

import jakarta.validation.constraints.*;

public record CommentRequest(
        @NotBlank(message = "Comment is required")
        @Size(max = 1000, message = "Comment too long")
        @Pattern(regexp = "^[^<>]*$", message = "Comment contains invalid characters")
        String comment) {
}
