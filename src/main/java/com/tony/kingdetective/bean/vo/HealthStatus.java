package com.tony.kingdetective.bean.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 健康状态响应
 * 
 * @author Tony Wang
 */
@Data
@Builder
public class HealthStatus {
    /** 状态：UP / DOWN */
    private String status;
    
    /** 数据库连接状态 */
    private Boolean databaseConnectivity;
    
    /** 内存状态 */
    private Boolean memoryStatus;

    /** JVM 已用内存（字节） */
    private Long usedMemoryBytes;

    /** JVM 最大内存（字节） */
    private Long maxMemoryBytes;

    /** 应用运行时长（秒） */
    private Long uptimeSeconds;

    /** 当前版本 */
    private String version;
    
    /** 时间戳 */
    private Long timestamp;
}
