package com.tony.kingdetective.exception;

/**
 * Security Exception
 * 
 * <p>Custom exception for security-related errors including:</p>
 * <ul>
 *   <li>IP blacklist violations</li>
 *   <li>Defense mode blocks</li>
 *   <li>Authentication failures</li>
 *   <li>Rate limit exceeded</li>
 * </ul>
 * 
 * @author King-Detective Team
 * @version 2.0.0
 * @since 2026-02-07
 */
public class SecurityException extends RuntimeException {
    
    private final SecurityErrorCode errorCode;
    
    public SecurityException(SecurityErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public SecurityErrorCode getErrorCode() {
        return errorCode;
    }
    
    public enum SecurityErrorCode {
        /**
         * IP is blacklisted
         */
        IP_BLACKLISTED,
        
        /**
         * Defense mode is active
         */
        DEFENSE_MODE_ACTIVE,
        
        /**
         * Too many failed login attempts
         */
        TOO_MANY_ATTEMPTS,
        
        /**
         * Rate limit exceeded
         */
        RATE_LIMIT_EXCEEDED,
        
        /**
         * Invalid credentials
         */
        INVALID_CREDENTIALS
    }
}
