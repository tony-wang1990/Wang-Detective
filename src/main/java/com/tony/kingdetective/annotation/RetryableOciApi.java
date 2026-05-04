package com.tony.kingdetective.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为可重试的 OCI API 调用
 * 使用此注解的方法在失败时会自动重试
 * 
 * @author Tony Wang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryableOciApi {
    /**
     * 最大重试次数（默认使用全局配置）
     */
    int maxAttempts() default 3;
    
    /**
     * 重试延迟（毫秒）
     */
    long delayMs() default 1000;
}
