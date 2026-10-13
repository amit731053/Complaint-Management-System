package com.cms.cms.dto;

import com.cms.cms.model.Complaint;

import java.time.LocalDateTime;

/** What the browser receives: no password, no nested user object. */
public record ComplaintDto(
        Long id, String title, String description, String status, String priority,
        String category, boolean hasComment, String adminComment,
        String userEmail, String userName,
        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static ComplaintDto from(Complaint c) {
        return new ComplaintDto(c.getId(), c.getTitle(), c.getDescription(), c.getStatus(),
                c.getPriority(), c.getCategory(), c.isHasComment(), c.getAdminComment(),
                c.getUser().getEmail(), c.getUser().getName(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
