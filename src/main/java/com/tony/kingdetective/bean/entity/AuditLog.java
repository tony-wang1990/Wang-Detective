package com.tony.kingdetective.bean.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * 记录系统关键操作历史
 * 
 * @author Tony Wang
 */
@Data
@TableName("audit_log")
public class AuditLog {
    
    /** 主键ID */
    @TableId
    private String id;
    
    /** 用户ID */
    @TableField("user_id")
    private String userId;
    
    /** 用户名 */
    @TableField("username")
    private String username;
    
    /** 操作类型 */
    @TableField("operation")
    private String operation;
    
    /** 操作目标（如实例ID、配置ID等） */
    @TableField("target")
    private String target;
    
    /** 操作详情（JSON格式） */
    @TableField("details")
    private String details;
    
    /** 是否成功 */
    @TableField("success")
    private Boolean success;
    
    /** 错误消息（如果失败） */
    @TableField("error_message")
    private String errorMessage;
    
    /** IP地址 */
    @TableField("ip_address")
    private String ipAddress;
    
    /** 用户代理 */
    @TableField("user_agent")
    private String userAgent;
    
    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
