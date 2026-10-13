package com.cms.cms.controller;

import com.cms.cms.dto.CommentRequest;
import com.cms.cms.dto.ComplaintDto;
import com.cms.cms.dto.ComplaintRequest;
import com.cms.cms.model.User;
import com.cms.cms.repository.UserRepository;
import com.cms.cms.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Admins see/handle every complaint; users only their own.
 * Authorization is enforced here and in ComplaintService, never in the browser.
 */
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService service;
    private final UserRepository users;

    public ComplaintController(ComplaintService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    private User me(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDto create(@Valid @RequestBody ComplaintRequest req, Authentication auth) {
        return ComplaintDto.from(service.create(me(auth), req));
    }

    @GetMapping
    public List<ComplaintDto> list(Authentication auth) {
        return service.listFor(me(auth)).stream().map(ComplaintDto::from).toList();
    }

    @GetMapping("/{id}")
    public ComplaintDto get(@PathVariable Long id, Authentication auth) {
        return ComplaintDto.from(service.getFor(id, me(auth)));
    }

    @PutMapping("/{id}")
    public ComplaintDto updateStatus(@PathVariable Long id, @RequestParam String status,
                                     Authentication auth) {
        return ComplaintDto.from(service.updateStatus(id, status, me(auth)));
    }

    @PutMapping("/{id}/edit")
    public ComplaintDto edit(@PathVariable Long id, @Valid @RequestBody ComplaintRequest req,
                             Authentication auth) {
        return ComplaintDto.from(service.edit(id, req, me(auth)));
    }

    @PostMapping("/{id}/comment")
    public ComplaintDto comment(@PathVariable Long id, @Valid @RequestBody CommentRequest req,
                                Authentication auth) {
        return ComplaintDto.from(service.comment(id, req.comment(), me(auth)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id); // URL is restricted to ADMIN in SecurityConfig
    }
}
