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
 * IP Blacklist Entity
 * 
 * @author antigravity-ai
 */
@TableName(value = "ip_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpBlacklist implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String ipAddress;
    
    private String reason;
    
    private String bannedBy;
    
    private LocalDateTime createTime;
    
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
