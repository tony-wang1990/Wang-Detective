package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.entity.AuditLog;
import com.tony.kingdetective.service.IAuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops/audit")
public class OpsAuditController {
    private final IAuditLogService auditLogService;

    public OpsAuditController(IAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/recent")
    public ResponseData<List<AuditLog>> recent(@RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
        return ResponseData.successData(auditLogService.recent(limit));
    }
}
