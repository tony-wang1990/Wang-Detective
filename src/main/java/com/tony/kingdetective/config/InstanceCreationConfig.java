package com.tony.kingdetective.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Instance Creation Configuration
 * 实例创建配置
 * 
 * @author Tony Wang
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "instance-creation")
public class InstanceCreationConfig {
    
    /**
     * 抢机重试间隔（秒）
     * Retry interval for instance creation attempts (seconds)
     * Default: 80 seconds
     */
    private int retryIntervalSeconds = 80;
}
