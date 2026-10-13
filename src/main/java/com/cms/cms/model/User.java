package com.cms.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    /** Always stored trimmed + lower-case. Unique at DB level. */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt hash. Never serialised to JSON. */
    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 10)
    private String role; // USER / ADMIN
}
