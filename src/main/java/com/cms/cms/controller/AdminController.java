package com.cms.cms.controller;

import com.cms.cms.dto.UserDto;
import com.cms.cms.repository.UserRepository;
import com.cms.cms.service.ComplaintService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Everything under /api/admin/** requires ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;
    private final ComplaintService complaints;

    public AdminController(UserRepository users, ComplaintService complaints) {
        this.users = users;
        this.complaints = complaints;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return users.findAll().stream().map(UserDto::from).toList();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return complaints.stats();
    }
}
