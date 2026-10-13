package com.cms.cms.dto;

import jakarta.validation.constraints.*;

/** Used for both create and edit. Status/owner are never taken from the client. */
public record ComplaintRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title too long")
        @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Title may contain only letters, numbers and spaces")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description too long")
        @Pattern(regexp = "^[^<>]*$", message = "Description contains invalid characters")
        String description,

        @NotBlank(message = "Category is required")
        @Pattern(regexp = "^(it_network|hardware|maintenance|hostel|library|accounts|sports|other)$",
                message = "Invalid category")
        String category,

        @NotBlank(message = "Priority is required")
        @Pattern(regexp = "^(?i)(low|medium|high|critical)$", message = "Invalid priority")
        String priority) {
}
