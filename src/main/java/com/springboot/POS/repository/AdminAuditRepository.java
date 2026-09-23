package com.springboot.POS.repository;

import com.springboot.POS.modal.AdminAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditRepository extends JpaRepository<AdminAudit, Long> {
    Page<AdminAudit> findByEntityTypeIgnoreCase(String entityType, Pageable pageable);
    Page<AdminAudit> findByAdminId(Long adminId, Pageable pageable);
}
