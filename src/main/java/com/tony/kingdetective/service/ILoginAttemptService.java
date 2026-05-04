package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tony.kingdetective.bean.entity.LoginAttempt;

/**
 * Login Attempt Service
 * 
 * @author antigravity-ai
 */
public interface ILoginAttemptService extends IService<LoginAttempt> {
    
    /**
     * Record login failure
     */
    void recordFailure(String ipAddress);
    
    /**
     * Get attempt count for IP
     */
    int getAttemptCount(String ipAddress);
    
    /**
     * Clear attempts for IP (on successful login)
     */
    void clearAttempts(String ipAddress);
    
    /**
     * Clean expired attempts (older than 30 minutes)
     */
    void cleanExpiredAttempts();
}
