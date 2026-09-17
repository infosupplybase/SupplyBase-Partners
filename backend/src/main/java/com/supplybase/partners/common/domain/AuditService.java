package com.supplybase.partners.common.domain;

import org.springframework.stereotype.Service;

/** Thin wrapper so services record "who did what, when, and why" in one place. */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(Long actorUserId, String action, String entityType, Object entityId, String reason) {
        repository.save(new AuditLog(actorUserId, action, entityType, String.valueOf(entityId), reason));
    }
}
