package com.cms.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Complaint {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 2000)
    private String description;

    private String status;   // PENDING, IN_PROGRESS, RESOLVED
    private String priority; // low, medium, high, critical
    private String category;
    private boolean hasComment;

    @Column(length = 1000)
    private String adminComment;

    private String userEmail;

    /** Owner. Never serialised directly (use ComplaintDto). */
    @JsonIgnore
    @ManyToOne(optional = false)
    private User user;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
