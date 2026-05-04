package com.tony.kingdetective.utils;

import com.tony.kingdetective.exception.OciException;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 输入验证工具类
 * 提供统一的输入验证方法
 * 
 * @author Tony Wang
 */
@Slf4j
public class InputValidator {
    
    // URL 正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[\\w.-]+(:\\d+)?(/.*)?$"
    );
    
    // IP 地址正则表达式
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // CIDR 正则表达式
    private static final Pattern CIDR_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/([0-9]|[1-2][0-9]|3[0-2])$"
    );
    
    // 主机名正则表达式
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );
    
    /**
     * 验证 URL格式
     *
     * @param url URL字符串
     * @throws OciException 如果格式无效
     */
    public static void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new OciException(-400, "URL 不能为空");
        }
        
        if (!URL_PATTERN.matcher(url).matches()) {
            throw new OciException(-400, 
                "无效的 URL 格式\n\n" +
                "必须以 http:// 或 https:// 开头\n\n" +
                "示例：\n" +
                "• http://192.168.1.100:6080\n" +
                "• https://vnc.example.com"
            );
        }
        
        log.debug("URL 验证通过: {}", url);
    }
    
    /**
     * 验证端口号
     *
     * @param port 端口号
     * @throws OciException 如果端口无效
     */
    public static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new OciException(-400, "端口号必须在 1-65535 之间");
        }
        
        log.debug("端口验证通过: {}", port);
    }
    
    /**
     * 验证 IP 地址
     *
     * @param ip IP地址字符串
     * @throws OciException 如果IP无效
     */
    public static void validateIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            throw new OciException(-400, "IP 地址不能为空");
        }
        
        if (!IP_PATTERN.matcher(ip).matches()) {
            throw new OciException(-400, 
                "无效的 IP 地址格式\n\n" +
                "示例: 192.168.1.100"
            );
        }
        
        log.debug("IP 地址验证通过: {}", ip);
    }
    
    /**
     * 验证 CIDR 块
     *
     * @param cidr CIDR字符串
     * @throws OciException 如果CIDR无效
     */
    public static void validateCidr(String cidr) {
        if (cidr == null || cidr.trim().isEmpty()) {
            throw new OciException(-400, "CIDR 块不能为空");
        }
        
        if (!CIDR_PATTERN.matcher(cidr).matches()) {
            throw new OciException(-400, 
                "无效的 CIDR 格式\n\n" +
                "示例: 10.0.0.0/16 或 192.168.1.0/24"
            );
        }
        
        log.debug("CIDR 验证通过: {}", cidr);
    }
    
    /**
     * 验证主机名
     *
     * @param hostname 主机名
     * @throws OciException 如果主机名无效
     */
    public static void validateHostname(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new OciException(-400, "主机名不能为空");
        }
        
        if (!HOSTNAME_PATTERN.matcher(hostname).matches()) {
            throw new OciException(-400, 
                "无效的主机名格式\n\n" +
                "示例: example.com 或 server01"
            );
        }
        
        log.debug("主机名验证通过: {}", hostname);
    }
    
    /**
     * 验证用户名（SSH、数据库等）
     *
     * @param username 用户名
     * @throws OciException 如果用户名无效
     */
    public static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new OciException(-400, "用户名不能为空");
        }
        
        if (username.length() < 2 || username.length() > 32) {
            throw new OciException(-400, "用户名长度必须在 2-32 个字符之间");
        }
        
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new OciException(-400, 
                "用户名只能包含字母、数字、下划线和连字符"
            );
        }
        
        log.debug("用户名验证通过: {}", username);
    }
    
    /**
     * 验证密码强度
     *
     * @param password 密码
     * @param minLength 最小长度
     * @throws OciException 如果密码强度不够
     */
    public static void validatePassword(String password, int minLength) {
        if (password == null || password.isEmpty()) {
            throw new OciException(-400, "密码不能为空");
        }
        
        if (password.length() < minLength) {
            throw new OciException(-400, 
                String.format("密码长度至少为 %d 个字符\n\n建议使用强密码以提高安全性", minLength)
            );
        }
        
        log.debug("密码验证通过（长度: {}）", password.length());
    }
    
    /**
     * 验证字符串长度
     *
     * @param text 文本
     * @param fieldName 字段名称
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @throws OciException 如果长度不符合要求
     */
    public static void validateLength(String text, String fieldName, int minLength, int maxLength) {
        if (text == null) {
            throw new OciException(-400, fieldName + " 不能为空");
        }
        
        if (text.length() < minLength || text.length() > maxLength) {
            throw new OciException(-400, 
                String.format("%s 长度必须在 %d-%d 个字符之间", fieldName, minLength, maxLength)
            );
        }
        
        log.debug("长度验证通过: {} (长度: {})", fieldName, text.length());
    }
    
    /**
     * 验证非空字符串
     *
     * @param text 文本
     * @param fieldName 字段名称
     * @throws OciException 如果为空
     */
    public static void validateNotEmpty(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new OciException(-400, fieldName + " 不能为空");
        }
        
        log.debug("非空验证通过: {}", fieldName);
    }
}
