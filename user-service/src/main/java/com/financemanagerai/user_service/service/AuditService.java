package com.financemanagerai.user_service.service;

import com.financemanagerai.user_service.entity.AuditLog;
import com.financemanagerai.user_service.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(String action, String performedBy, String targetUser) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .performedBy(performedBy)
                .targetUser(targetUser)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }
}