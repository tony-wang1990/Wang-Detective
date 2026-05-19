package com.tony.kingdetective.telegram.storage;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH Connection Storage
 * Thread-safe singleton storage for managing SSH connection info
 *
 * fix: 密码改用 char[] 存储，避免 String 常量池导致明文密码长期驻留堆内存；
 *      连接记录使用完毕后调用 clearPassword() 主动抹除
 *
 * @author yohann
 */
public class SshConnectionStorage {

    private static final SshConnectionStorage INSTANCE = new SshConnectionStorage();

    /**
     * SSH Connection Info
     * fix: password 从 String 改为 char[]，使用后可以主动清零
     */
    public static class SshInfo {
        private final String host;
        private final int port;
        private final String username;
        /** fix: 使用 char[] 代替 String，用完后可调用 clearPassword() 清零 */
        private final char[] password;
        private volatile long lastUsed;

        public SshInfo(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password != null ? password.toCharArray() : new char[0];
            this.lastUsed = System.currentTimeMillis();
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        /**
         * 获取密码（转为 String 供 SSH 库使用）
         * 注意：调用方不应长期持有此 String 引用
         */
        public String getPassword() {
            return new String(password);
        }

        /**
         * fix: 主动清零密码内存，在连接不再需要时调用
         */
        public void clearPassword() {
            Arrays.fill(password, '\0');
        }

        public long getLastUsed() {
            return lastUsed;
        }

        public void updateLastUsed() {
            this.lastUsed = System.currentTimeMillis();
        }
    }

    // Store SSH connection info: chatId -> SshInfo
    private final Map<Long, SshInfo> connections = new ConcurrentHashMap<>();

    private SshConnectionStorage() {
    }

    public static SshConnectionStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Save SSH connection info
     */
    public void saveConnection(long chatId, String host, int port, String username, String password) {
        // fix: 替换旧连接时，先清零旧密码
        SshInfo old = connections.get(chatId);
        if (old != null) {
            old.clearPassword();
        }
        connections.put(chatId, new SshInfo(host, port, username, password));
    }

    /**
     * Get SSH connection info
     */
    public SshInfo getConnection(long chatId) {
        SshInfo info = connections.get(chatId);
        if (info != null) {
            info.updateLastUsed();
        }
        return info;
    }

    /**
     * Check if connection exists
     */
    public boolean hasConnection(long chatId) {
        return connections.containsKey(chatId);
    }

    /**
     * Remove SSH connection info
     * fix: 移除时主动清零密码内存
     */
    public void removeConnection(long chatId) {
        SshInfo info = connections.remove(chatId);
        if (info != null) {
            info.clearPassword();
        }
    }

    /**
     * Clear all connections
     * fix: 清空时逐个清零密码
     */
    public void clearAll() {
        connections.values().forEach(SshInfo::clearPassword);
        connections.clear();
    }

    /**
     * Clean up expired connections (not used for more than 30 minutes)
     * fix: 清理过期连接时也清零密码
     */
    public void cleanExpiredConnections() {
        long now = System.currentTimeMillis();
        long timeout = 30 * 60 * 1000L; // 30 minutes

        connections.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue().getLastUsed() > timeout;
            if (expired) {
                entry.getValue().clearPassword();
            }
            return expired;
        });
    }
}
