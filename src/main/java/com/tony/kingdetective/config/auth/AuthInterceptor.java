package com.tony.kingdetective.config.auth;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.AdminCredentialService;
import com.tony.kingdetective.service.IIpBlacklistService;
import com.tony.kingdetective.service.ILoginAttemptService;
import com.tony.kingdetective.service.IOciKvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * @author: Tony Wang
 * @date: 2024/3/30 18:03
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    private static final String DEFENSE_MODE_KEY = "defense_mode_enabled";

    private final AdminCredentialService adminCredentialService;

    public AuthInterceptor(AdminCredentialService adminCredentialService) {
        this.adminCredentialService = adminCredentialService;
    }

    List<String> noTokenList = Arrays.asList(
            "/api/sys/login",
            "/api/sys/getEnableMfa",
            "/api/sys/googleLogin",
            "/api/sys/getGoogleClientId"
    );

    // AI 聊天接口无需 Token（前端携带 Token 时也会正常通过鉴权，此处放行未登录访问的情况）
    // fix: /chat/** 路径未加 /api 前缀，鉴权拦截器仅拦截 /api/**，故无需特殊处理
    // 但保留此注释，提示后续维护者：若将 /chat 迁移到 /api/chat 需同步更新 noTokenList


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 WebSocket 握手请求
        if ("GET".equalsIgnoreCase(request.getMethod()) && "websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            return true;
        }

        // 放行预检请求（OPTIONS）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK); // 直接返回 200 状态码
            return true;
        }
        
        // === 安全检查 ===
        String clientIp = getClientIp(request);
        
        // 1. 检查防御模式
        if (isDefenseModeEnabled()) {
            log.warn("Defense mode is enabled, blocking request from IP: {}", clientIp);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"code\":403,\"msg\":\"Defense mode enabled. Access denied.\"}");
            return false;
        }
        
        // 2. 检查IP黑名单
        IIpBlacklistService blacklistService = SpringUtil.getBean(IIpBlacklistService.class);
        if (blacklistService.isBlacklisted(clientIp)) {
            log.warn("IP {} is blacklisted", clientIp);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"code\":403,\"msg\":\"IP blocked\"}");
            return false;
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (request.getRequestURI().contains("/api") && !noTokenList.contains(request.getRequestURI())) {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7); // 去掉"Bearer "前缀
                // 验证token（这里可以调用你的验证逻辑）
                boolean isValid = validateToken(token);
                if (isValid) {
                    return true; // 继续处理请求
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    throw new OciException(401, "无权限");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                throw new OciException(401, "无权限");
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    private boolean validateToken(String token) {
        return adminCredentialService.verifyToken(token);
    }
    
    /**
     * Check if defense mode is enabled
     */
    private boolean isDefenseModeEnabled() {
        try {
            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            OciKv defenseModeKv = kvService.getByKey(DEFENSE_MODE_KEY);
            return defenseModeKv != null && "true".equals(defenseModeKv.getValue());
        } catch (Exception e) {
            log.error("Failed to check defense mode", e);
            return false;
        }
    }
    
    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况 (X-Forwarded-For可能包含多个IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
