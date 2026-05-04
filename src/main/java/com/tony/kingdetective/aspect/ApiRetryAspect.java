package com.tony.kingdetective.aspect;

import com.oracle.bmc.model.BmcException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * API 调用重试切面
 * 对 Oracle Cloud API 调用失败时自动重试
 * 
 * @author Tony Wang
 */
@Slf4j
@Aspect
@Component
public class ApiRetryAspect {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;  // 1秒
    private static final double BACKOFF_MULTIPLIER = 2.0;  // 每次重试延迟翻倍
    
    /**
     * 自动重试 OCI API 调用
     */
    @Around("@annotation(com.tony.kingdetective.annotation.RetryableOciApi)")
    public Object retryOciApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;
        
        while (true) {
            attempt++;
            try {
                log.debug("执行 API 调用: {} (尝试 {}/{})", methodName, attempt, MAX_RETRY_ATTEMPTS);
                Object result = joinPoint.proceed();
                
                if (attempt > 1) {
                    log.info("API 调用成功: {} (重试 {} 次后成功)", methodName, attempt - 1);
                }
                
                return result;
                
            } catch (BmcException e) {
                boolean shouldRetry = shouldRetry(e, attempt);
                
                if (shouldRetry) {
                    log.warn("API 调用失败: {}, 错误码: {}, 将在 {}ms 后重试 ({}/{})",
                            methodName, e.getStatusCode(), backoffMs, attempt, MAX_RETRY_ATTEMPTS);
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                    
                    // 指数退避
                    backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
                    
                } else {
                    log.error("API 调用失败: {}, 不再重试 (尝试 {} 次)", methodName, attempt);
                    throw e;
                }
                
            } catch (Exception e) {
                // 非 BmcException 的异常不重试
                log.error("API 调用发生非预期异常: {}", methodName, e);
                throw e;
            }
        }
    }
    
    /**
     * 判断是否应该重试
     *
     * @param e BmcException
     * @param attempt 当前尝试次数
     * @return true 如果应该重试
     */
    private boolean shouldRetry(BmcException e, int attempt) {
        // 已达最大重试次数
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            return false;
        }
        
        int statusCode = e.getStatusCode();
        
        // 可重试的状态码
        // 429: Too Many Requests (速率限制)
        // 500: Internal Server Error (服务器内部错误)
        // 502: Bad Gateway (网关错误)
        // 503: Service Unavailable (服务不可用)
        // 504: Gateway Timeout (网关超时)
        return statusCode == 429 ||
               statusCode == 500 ||
               statusCode == 502 ||
               statusCode == 503 ||
               statusCode == 504;
    }
}
