package com.springboot.POS.service;

import com.springboot.POS.modal.AdminAudit;
import com.springboot.POS.repository.AdminAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditRepository repository;

    public void record(Long adminId, String action, String entityType, Long entityId, String detail) {
        try {
            repository.save(AdminAudit.builder()
                    .adminId(adminId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .detail(detail)
                    .build());
        } catch (Exception ex) {
            // Audit must never break the admin action itself (showcase-safe).
            org.slf4j.LoggerFactory.getLogger(AdminAuditService.class)
                    .warn("Failed to record admin audit {} on {} {}: {}", action, entityType, entityId, ex.getMessage());
        }
    }

    public Page<AdminAudit> list(String entityType, Long adminId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pr = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (entityType != null && !entityType.isBlank()) return repository.findByEntityTypeIgnoreCase(entityType, pr);
        if (adminId != null) return repository.findByAdminId(adminId, pr);
        return repository.findAll(pr);
    }
}
