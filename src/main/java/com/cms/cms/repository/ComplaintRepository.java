package com.cms.cms.repository;

import com.cms.cms.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByIdAsc();
    List<Complaint> findByUserIdOrderByIdAsc(Long userId);
    long countByStatus(String status);
}
