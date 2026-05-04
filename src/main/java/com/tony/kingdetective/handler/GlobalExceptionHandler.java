package com.tony.kingdetective.handler;

import com.oracle.bmc.model.BmcException;
import com.tony.kingdetective.bean.vo.ErrorResponse;
import com.tony.kingdetective.exception.OciException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

/**
 * 全局异常处理器
 * 统一处理所有异常并返回标准错误响应
 * 
 * @author Tony Wang
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理自定义 OCI 异常
     */
    @ExceptionHandler(OciException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleOciException(OciException e) {
        log.error("OCI 操作异常: code={}, message={}", e.getCode(), e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * 处理 Oracle Cloud SDK 异常
     */
    @ExceptionHandler(BmcException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleBmcException(BmcException e) {
        log.error("Oracle Cloud SDK 异常: statusCode={}, serviceCode={}, message={}",
                e.getStatusCode(), e.getServiceCode(), e.getMessage(), e);
        
        String userMessage = formatBmcExceptionMessage(e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getStatusCode())
                .message(userMessage)
                .details(e.getServiceCode())
                .timestamp(System.currentTimeMillis())
                .build();
        
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
        
        return ResponseEntity
                .status(status)
                .body(response);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数验证失败: {}", e.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .code(400)
                .message("参数验证失败: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException e) {
        log.error("发生空指针异常", e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(500)
                .message("系统内部错误：数据为空")
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation failed: {}", message);

        ErrorResponse response = ErrorResponse.builder()
                .code(400)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle SPA routing (404 -> index.html)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFoundException(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Forward to index.html for SPA support
        request.getRequestDispatcher("/index.html").forward(request, response);
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("发生未预期的异常", e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(500)
                .message("系统内部错误：" + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
    
    /**
     * 格式化 BMC 异常消息为用户友好的文本
     */
    private String formatBmcExceptionMessage(BmcException e) {
        int statusCode = e.getStatusCode();
        String serviceCode = e.getServiceCode();
        
        // 根据常见错误码返回用户友好的消息
        if (statusCode == 401) {
            return "认证失败：请检查 API 密钥配置是否正确";
        } else if (statusCode == 404) {
            return "资源不存在：请检查资源 ID 是否正确";
        } else if (statusCode == 429) {
            return "请求过于频繁：请稍后再试";
        } else if (statusCode == 500) {
            return "Oracle Cloud 服务器错误：请稍后重试";
        } else if (statusCode == 503) {
            return "Oracle Cloud 服务暂时不可用：请稍后重试";
        } else if ("LimitExceeded".equals(serviceCode)) {
            return "配额不足：当前区域资源已达上限";
        } else if ("InsufficientHostCapacity".equals(serviceCode)) {
            return "主机容量不足：当前区域暂无可用资源";
        } else {
            return e.getMessage();
        }
    }
}
