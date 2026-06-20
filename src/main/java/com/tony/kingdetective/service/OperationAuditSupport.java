package com.tony.kingdetective.service;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class OperationAuditSupport {

    private final IAuditLogService auditLogService;

    public OperationAuditSupport(IAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public void run(String operation, String target, String details, Runnable action) {
        supply(operation, target, details, () -> {
            action.run();
            return null;
        });
    }

    public <T> T supply(String operation, String target, String details, Supplier<T> action) {
        String safeTarget = sanitize(target);
        String safeDetails = sanitize(details);
        try {
            T result = action.get();
            auditLogService.logSuccess(null, operation, safeTarget, safeDetails);
            return result;
        } catch (RuntimeException e) {
            auditLogService.logFailure(null, operation, safeTarget, sanitize(e.getMessage()));
            throw e;
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replace('\n', ' ').replace('\r', ' ');
        return sanitized.length() > 500 ? sanitized.substring(0, 500) + "..." : sanitized;
    }
}
