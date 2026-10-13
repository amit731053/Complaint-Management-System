package com.cms.cms.service;

import com.cms.cms.dto.ComplaintRequest;
import com.cms.cms.model.Complaint;
import com.cms.cms.model.User;
import com.cms.cms.repository.ComplaintRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ComplaintService {

    public static final Set<String> STATUSES = Set.of("PENDING", "IN_PROGRESS", "RESOLVED");

    private final ComplaintRepository repo;

    public ComplaintService(ComplaintRepository repo) {
        this.repo = repo;
    }

    private static boolean isAdmin(User u) {
        return "ADMIN".equals(u.getRole());
    }

    private Complaint find(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
    }

    /** Owner or admin may access; otherwise 403. */
    private Complaint findAccessible(Long id, User me) {
        Complaint c = find(id);
        if (!isAdmin(me) && !c.getUser().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        return c;
    }

    public Complaint create(User me, ComplaintRequest r) {
        Complaint c = new Complaint();
        c.setTitle(r.title().trim());
        c.setDescription(r.description().trim());
        c.setCategory(r.category());
        c.setPriority(r.priority().toLowerCase());
        c.setStatus("PENDING");
        c.setUser(me);
        c.setUserEmail(me.getEmail());
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public List<Complaint> listFor(User me) {
        return isAdmin(me) ? repo.findAllByOrderByIdAsc() : repo.findByUserIdOrderByIdAsc(me.getId());
    }

    @Transactional(readOnly = true)
    public Complaint getFor(Long id, User me) {
        return findAccessible(id, me);
    }

    public Complaint updateStatus(Long id, String rawStatus, User me) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase();
        if (!STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        Complaint c = findAccessible(id, me);
        // A normal user may only close their own complaint.
        if (!isAdmin(me) && !"RESOLVED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        c.setStatus(status);
        return repo.save(c);
    }

    public Complaint edit(Long id, ComplaintRequest r, User me) {
        Complaint c = findAccessible(id, me);
        c.setTitle(r.title().trim());
        c.setDescription(r.description().trim());
        c.setCategory(r.category());
        c.setPriority(r.priority().toLowerCase());
        return repo.save(c);
    }

    public Complaint comment(Long id, String text, User me) {
        if (!isAdmin(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        Complaint c = find(id);
        c.setHasComment(true);
        c.setAdminComment(text.trim());
        return repo.save(c);
    }

    public void delete(Long id) {
        repo.delete(find(id));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        return Map.of(
                "total", repo.count(),
                "pending", repo.countByStatus("PENDING"),
                "resolved", repo.countByStatus("RESOLVED"));
    }
}
