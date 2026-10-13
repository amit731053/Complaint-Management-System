package com.cms.cms.controller;

import com.cms.cms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Used by hosting platforms / load balancers to check the app and its database are alive. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final UserRepository users;

    public HealthController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        try {
            users.count();
            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN"));
        }
    }
}
