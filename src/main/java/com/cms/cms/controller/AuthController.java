package com.cms.cms.controller;

import com.cms.cms.config.LoginRateLimiter;
import com.cms.cms.dto.LoginRequest;
import com.cms.cms.dto.SignupRequest;
import com.cms.cms.dto.UserDto;
import com.cms.cms.model.User;
import com.cms.cms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepository;
    private final LoginRateLimiter limiter;

    public AuthController(UserRepository users, PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          SecurityContextRepository contextRepository,
                          LoginRateLimiter limiter) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.contextRepository = contextRepository;
        this.limiter = limiter;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "EMAIL_EXISTS"));
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(req.password()));
        u.setRole("USER"); // role is never taken from the client
        try {
            users.save(u);
        } catch (DataIntegrityViolationException e) { // two signups racing on the same email
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "EMAIL_EXISTS"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "SUCCESS"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest request, HttpServletResponse response) {
        String email = req.email().trim().toLowerCase();
        String ip = request.getRemoteAddr();

        long wait = limiter.retryAfterSeconds(email, ip);
        if (wait > 0) {
            long minutes = (wait + 59) / 60;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(wait))
                    .body(Map.of("error", "Too many failed attempts. Try again in " + minutes + " minute(s)."));
        }

        try {
            Authentication auth = authManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, req.password()));

            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            if (request.getSession(false) != null) {
                request.changeSessionId(); // prevent session fixation
            }
            contextRepository.saveContext(ctx, request, response);

            limiter.recordSuccess(email);
            User u = users.findByEmail(auth.getName()).orElseThrow();
            return ResponseEntity.ok(UserDto.from(u));
        } catch (AuthenticationException e) {
            limiter.recordFailure(email, ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return users.findByEmail(auth.getName())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(UserDto.from(u)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication auth, HttpServletRequest request,
                                       HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, auth);
        return ResponseEntity.noContent().build();
    }
}
