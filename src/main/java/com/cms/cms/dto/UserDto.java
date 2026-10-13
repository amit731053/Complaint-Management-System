package com.cms.cms.dto;

import com.cms.cms.model.User;

/** Safe view of a user: no password. */
public record UserDto(Long id, String name, String email, String role) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
