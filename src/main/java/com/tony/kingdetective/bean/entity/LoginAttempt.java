package com.tony.kingdetective.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Login Attempt Entity
 * Track login failures for auto-ban
 * 
 * @author antigravity-ai
 */
@TableName(value = "login_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String ipAddress;
    
    private Integer attemptCount;
    
    private LocalDateTime lastAttempt;
    
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
