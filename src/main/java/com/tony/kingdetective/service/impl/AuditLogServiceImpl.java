package com.tony.kingdetective.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.AuditLog;
import com.tony.kingdetective.mapper.AuditLogMapper;
import com.tony.kingdetective.service.IAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AuditLogServiceImpl implements IAuditLogService {
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void log(AuditLog auditLog) {
        if (auditLog == null) {
            return;
        }
        try {
            if (auditLog.getId() == null) {
                auditLog.setId(IdUtil.fastSimpleUUID());
            }
            if (auditLog.getCreateTime() == null) {
                auditLog.setCreateTime(LocalDateTime.now());
            }
            fillRequest(auditLog);
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Override
    public void logSuccess(String userId, String operation, String target, String details) {
        AuditLog auditLog = base(userId, operation, target);
        auditLog.setDetails(details);
        auditLog.setSuccess(true);
        log(auditLog);
    }

    @Override
    public void logFailure(String userId, String operation, String target, String error) {
        AuditLog auditLog = base(userId, operation, target);
        auditLog.setSuccess(false);
        auditLog.setErrorMessage(error);
        log(auditLog);
    }

    @Override
    public List<AuditLog> recent(int limit) {
        int size = limit <= 0 ? 100 : Math.min(limit, 500);
        return auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getCreateTime)
                .last("LIMIT " + size));
    }

    private AuditLog base(String userId, String operation, String target) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId == null || userId.isBlank() ? currentUserId() : userId);
        auditLog.setUsername("web");
        auditLog.setOperation(operation);
        auditLog.setTarget(target);
        auditLog.setCreateTime(LocalDateTime.now());
        return auditLog;
    }

    private void fillRequest(AuditLog auditLog) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        auditLog.setIpAddress(clientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        if (auditLog.getUserId() == null || auditLog.getUserId().isBlank()) {
            auditLog.setUserId(currentUserId());
        }
    }

    private String currentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "web";
        }
        String authorization = attributes.getRequest().getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "web";
        }
        try {
            Object account = JWTUtil.parseToken(authorization.substring(7)).getPayload("account");
            return account == null ? "web" : String.valueOf(account);
        } catch (Exception e) {
            return "web";
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
