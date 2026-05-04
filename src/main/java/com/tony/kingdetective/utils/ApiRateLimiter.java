package com.tony.kingdetective.utils;

import com.google.common.util.concurrent.RateLimiter;
import com.tony.kingdetective.exception.OciException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * API 速率限制器
 * 使用 Token Bucket 算法防止 API 调用过于频繁
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class ApiRateLimiter {
    
    // 不同API的速率限制配置（每秒允许的请求数）
    private static final double QUOTA_QUERY_RATE = 2.0;        // 配额查询: 2次/秒
    private static final double COST_QUERY_RATE = 1.0;         // 成本查询: 1次/秒
    private static final double INSTANCE_CREATE_RATE = 0.5;    // 创建实例: 0.5次/秒(2秒1次)
    private static final double INSTANCE_ACTION_RATE = 2.0;    // 实例操作: 2次/秒
    private static final double NETWORK_CONFIG_RATE = 1.0;     // 网络配置: 1次/秒
    private static final double DEFAULT_RATE = 5.0;            // 默认: 5次/秒
    
    // 用户级别速率限制（每个用户每秒最多请求数）
    private static final double USER_RATE_LIMIT = 10.0;        // 10次/秒
    
    // 速率限制器缓存
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    
    // 超时时间（秒）
    private static final int TIMEOUT_SECONDS = 5;
    
    /**
     * 配额查询速率限制
     */
    public void acquireQuotaQuery(String userId) {
        acquire("quota_query", QUOTA_QUERY_RATE, userId);
    }
    
    /**
     * 成本查询速率限制
     */
    public void acquireCostQuery(String userId) {
        acquire("cost_query", COST_QUERY_RATE, userId);
    }
    
    /**
     * 创建实例速率限制
     */
    public void acquireInstanceCreate(String userId) {
        acquire("instance_create", INSTANCE_CREATE_RATE, userId);
    }
    
    /**
     * 实例操作速率限制
     */
    public void acquireInstanceAction(String userId) {
        acquire("instance_action", INSTANCE_ACTION_RATE, userId);
    }
    
    /**
     * 网络配置速率限制
     */
    public void acquireNetworkConfig(String userId) {
        acquire("network_config", NETWORK_CONFIG_RATE, userId);
    }
    
    /**
     * 通用API速率限制
     */
    public void acquireGeneral(String apiName, String userId) {
        acquire(apiName, DEFAULT_RATE, userId);
    }
    
    /**
     * 获取速率限制令牌
     *
     * @param apiName API名称
     * @param rate 每秒允许的请求数
     * @param userId 用户ID
     */
    private void acquire(String apiName, double rate, String userId) {
        // 1. 检查用户级别限制
        RateLimiter userLimiter = userLimiters.computeIfAbsent(
            userId,
            k -> RateLimiter.create(USER_RATE_LIMIT)
        );
        
        if (!userLimiter.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.warn("用户 {} 请求过于频繁，已触发限流", userId);
            throw new OciException(-429, "请求过于频繁，请稍后再试");
        }
        
        // 2. 检查API级别限制
        RateLimiter apiLimiter = limiters.computeIfAbsent(
            apiName,
            k -> RateLimiter.create(rate)
        );
        
        if (!apiLimiter.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.warn("API {} 调用过于频繁，用户: {}", apiName, userId);
            throw new OciException(-429, String.format("【%s】调用过于频繁，请稍后再试", apiName));
        }
        
        log.debug("API {} 速率限制检查通过，用户: {}", apiName, userId);
    }
    
    /**
     * 重置指定用户的速率限制
     *
     * @param userId 用户ID
     */
    public void resetUserLimit(String userId) {
        userLimiters.remove(userId);
        log.info("已重置用户 {} 的速率限制", userId);
    }
    
    /**
     * 重置指定API的速率限制
     *
     * @param apiName API名称
     */
    public void resetApiLimit(String apiName) {
        limiters.remove(apiName);
        log.info("已重置 API {} 的速率限制", apiName);
    }
    
    /**
     * 清空所有速率限制
     */
    public void resetAll() {
        limiters.clear();
        userLimiters.clear();
        log.info("已清空所有速率限制");
    }
}
