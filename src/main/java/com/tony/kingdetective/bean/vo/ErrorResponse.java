package com.tony.kingdetective.bean.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 统一错误响应
 * 
 * @author Tony Wang
 */
@Data
@Builder
public class ErrorResponse {
    /** 错误码 */
    private Integer code;
    
    /** 错误消息 */
    private String message;
    
    /** 详细信息(可选) */
    private String details;
    
    /** 时间戳 */
    private Long timestamp;
}
