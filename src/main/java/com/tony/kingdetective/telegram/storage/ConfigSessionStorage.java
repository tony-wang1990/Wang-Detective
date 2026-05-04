package com.tony.kingdetective.telegram.storage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration Session Storage
 * Manages different types of configuration sessions (VNC, Backup, etc.)
 * to avoid conflicts with AI chat and other features
 * 
 * @author yohann
 */
@Slf4j
public class ConfigSessionStorage {
    
    private static final ConfigSessionStorage INSTANCE = new ConfigSessionStorage();
    
    /**
     * Session state for each chat
     */
    private final Map<Long, SessionState> sessions = new ConcurrentHashMap<>();
    
    private ConfigSessionStorage() {
    }
    
    public static ConfigSessionStorage getInstance() {
        return INSTANCE;
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
        return sessions.containsKey(chatId);
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

    // ... existing methods ...

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
