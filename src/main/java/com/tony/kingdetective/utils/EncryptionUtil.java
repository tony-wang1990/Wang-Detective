package com.tony.kingdetective.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 敏感信息加密工具类
 * 使用 AES-256 加密算法
 * 
 * @author Tony Wang
 */
@Slf4j
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // 从环境变量获取密钥，如果不存在则自动生成并保存
    private static final String SECRET_KEY = getOrCreateSecretKey();
    
    private static final AES aes;
    
    static {
        try {
            // 确保密钥长度为32字节(256位)
            String key = SECRET_KEY.length() >= 32
                    ? SECRET_KEY.substring(0, 32)
                    : String.format("%-32s", SECRET_KEY).replace(' ', '0');
            
            aes = SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
            log.info("加密工具初始化成功，使用 AES-256 算法");
        } catch (Exception e) {
            log.error("加密工具初始化失败", e);
            throw new RuntimeException("加密工具初始化失败", e);
        }
    }
    
    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return Base64编码的密文
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            byte[] encrypted = aes.encrypt(plainText);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密失败: {}", plainText.substring(0, Math.min(10, plainText.length())) + "...", e);
            throw new RuntimeException("加密失败", e);
        }
    }
    
    /**
     * 解密字符串
     *
     * @param encryptedText Base64编码的密文
     * @return 明文
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = aes.decrypt(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }
    
    /**
     * 判断字符串是否为加密格式（Base64）
     *
     * @param text 待检测字符串
     * @return true如果是加密格式
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 加密敏感字段（智能加密 - 如果已经是加密格式则跳过）
     *
     * @param text 待加密文本
     * @return 加密后的文本
     */
    public static String encryptIfNeeded(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 如果已经加密，直接返回
        if (isEncrypted(text)) {
            return text;
        }
        
        return encrypt(text);
    }
    
    /**
     * 获取或创建密钥
     * 优先从环境变量读取，如果不存在则自动生成并保存到 .secret_key 文件
     */
    private static String getOrCreateSecretKey() {
        // 1. 尝试从环境变量读取
        String envKey = System.getenv("KING_DETECTIVE_SECRET_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            log.info("✅ 使用环境变量中的加密密钥");
            return ensureKeyLength(envKey);
        }
        
        // 2. 尝试从文件读取
        java.io.File keyFile = new java.io.File(System.getProperty("user.dir"), ".secret_key");
        if (keyFile.exists()) {
            try {
                String fileKey = java.nio.file.Files.readString(keyFile.toPath(), StandardCharsets.UTF_8).trim();
                if (!fileKey.isEmpty()) {
                    log.info("✅ 使用 .secret_key 文件中的加密密钥");
                    return ensureKeyLength(fileKey);
                }
            } catch (Exception e) {
                log.warn("读取 .secret_key 文件失败，将生成新密钥", e);
            }
        }
        
        // 3. 自动生成新密钥并保存
        String newKey = generateRandomKey();
        try {
            java.nio.file.Files.writeString(keyFile.toPath(), newKey, StandardCharsets.UTF_8);
            log.info("🔑 已自动生成并保存加密密钥到: {}", keyFile.getAbsolutePath());
            log.info("⚠️  请妥善保管此文件，丢失将无法解密已加密的数据！");
        } catch (Exception e) {
            log.error("保存密钥文件失败", e);
        }
        
        return newKey;
    }
    
    /**
     * 生成随机32字符密钥
     */
    private static String generateRandomKey() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        java.util.Random random = new java.security.SecureRandom();
        StringBuilder key = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            key.append(chars.charAt(random.nextInt(chars.length())));
        }
        return key.toString();
    }
    
    /**
     * 确保密钥长度为32字节
     */
    private static String ensureKeyLength(String key) {
        if (key.length() >= 32) {
            return key.substring(0, 32);
        } else {
            return String.format("%-32s", key).replace(' ', '0');
        }
    }
    
    private EncryptionUtil() {
        throw new AssertionError("工具类不应该被实例化");
    }
}
