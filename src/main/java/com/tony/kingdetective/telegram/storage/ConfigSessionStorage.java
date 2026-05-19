package com.tony.kingdetective.telegram.storage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Configuration Session Storage
 * Manages different types of configuration sessions (VNC, Backup, etc.)
 * to avoid conflicts with AI chat and other features
 *
 * fix: 增加 Session TTL 自动清理（30分钟过期），防止用户中途退出导致 chatId 永久占用
 *
 * @author yohann
 */
@Slf4j
public class ConfigSessionStorage {

    /** Session 超时时间：30分钟 */
    private static final long SESSION_TTL_MS = 30 * 60 * 1000L;

    private static final ConfigSessionStorage INSTANCE = new ConfigSessionStorage();

    /**
     * Session state for each chat
     */
    private final Map<Long, SessionState> sessions = new ConcurrentHashMap<>();

    private ConfigSessionStorage() {
        // fix: 启动定时清理任务，每5分钟扫描一次过期 session
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::cleanExpiredSessions, 5, 5, TimeUnit.MINUTES);
        log.info("ConfigSessionStorage initialized with TTL={}ms, cleaner interval=5min", SESSION_TTL_MS);
    }

    public static ConfigSessionStorage getInstance() {
        return INSTANCE;
    }

    /**
     * fix: 清理过期 session（超过 30 分钟未完成的操作自动取消）
     */
    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        int[] removed = {0};
        sessions.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue().getCreatedAt() > SESSION_TTL_MS;
            if (expired) {
                removed[0]++;
                log.info("Auto-expired session for chatId={}, type={}, age={}min",
                        entry.getKey(),
                        entry.getValue().getType(),
                        (now - entry.getValue().getCreatedAt()) / 60000);
            }
            return expired;
        });
        if (removed[0] > 0) {
            log.info("Session cleaner removed {} expired sessions", removed[0]);
        }
    }

    /**
     * Start a VNC configuration session
     */
    public void startVncConfig(long chatId) {
        SessionState state = new SessionState();
        state.setType(SessionType.VNC_CONFIG);
        sessions.put(chatId, state);
        log.debug("Started VNC config session for chatId: {}", chatId);
    }

    /**
     * Start a backup password input session
     */
    public void startBackupPassword(long chatId) {
        SessionState state = new SessionState();
        state.setType(SessionType.BACKUP_PASSWORD);
        sessions.put(chatId, state);
        log.debug("Started backup password session for chatId: {}", chatId);
    }

    /**
     * Start a restore password input session
     */
    public void startRestorePassword(long chatId, String messageId) {
        SessionState state = new SessionState();
        state.setType(SessionType.RESTORE_PASSWORD);
        state.getData().put("messageId", messageId);
        sessions.put(chatId, state);
        log.debug("Started restore password session for chatId: {}", chatId);
    }

    /**
     * Check if chat has an active session
     */
    public boolean hasActiveSession(long chatId) {
        SessionState state = sessions.get(chatId);
        if (state == null) return false;
        // fix: 同时检查是否已过期
        if (System.currentTimeMillis() - state.getCreatedAt() > SESSION_TTL_MS) {
            sessions.remove(chatId);
            log.debug("Session for chatId={} expired on access, auto-cleared", chatId);
            return false;
        }
        return true;
    }

    /**
     * Get session type
     */
    public SessionType getSessionType(long chatId) {
        SessionState state = sessions.get(chatId);
        return state != null ? state.getType() : null;
    }

    /**
     * Get session state
     */
    public SessionState getSessionState(long chatId) {
        return sessions.get(chatId);
    }

    /**
     * Clear session
     */
    public void clearSession(long chatId) {
        sessions.remove(chatId);
        log.debug("Cleared session for chatId: {}", chatId);
    }

    /**
     * Clear all sessions
     */
    public void clearAllSessions() {
        sessions.clear();
        log.info("Cleared all config sessions");
    }

    /**
     * Session state
     */
    @Data
    public static class SessionState {
        private SessionType type;
        private Map<String, Object> data = new ConcurrentHashMap<>();
        /** fix: 记录 session 创建时间，用于 TTL 过期判断 */
        private final long createdAt = System.currentTimeMillis();
    }

    /**
     * Start add account config session
     */
    public void startAddAccountConfig(long chatId) {
        SessionState state = new SessionState();
        state.setType(SessionType.ADD_ACCOUNT_CONFIG);
        sessions.put(chatId, state);
        log.debug("Started add account config session for chatId: {}", chatId);
    }

    /**
     * Start add account key session
     */
    public void startAddAccountKey(long chatId, Map<String, Object> previousData) {
        SessionState state = new SessionState();
        state.setType(SessionType.ADD_ACCOUNT_KEY);
        if (previousData != null) {
            state.setData(previousData);
        }
        sessions.put(chatId, state);
        log.debug("Started add account key session for chatId: {}", chatId);
    }

    /**
     * Start add account remark session
     */
    public void startAddAccountRemark(long chatId, Map<String, Object> previousData) {
        SessionState state = new SessionState();
        state.setType(SessionType.ADD_ACCOUNT_REMARK);
        if (previousData != null) {
            state.setData(previousData);
        }
        sessions.put(chatId, state);
        log.debug("Started add account remark session for chatId: {}", chatId);
    }

    /**
     * Session type enum
     */
    public enum SessionType {
        VNC_CONFIG,
        BACKUP_PASSWORD,
        RESTORE_PASSWORD,
        ADD_ACCOUNT_CONFIG,
        ADD_ACCOUNT_KEY,
        ADD_ACCOUNT_REMARK
    }
}
