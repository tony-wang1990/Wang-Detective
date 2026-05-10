package com.tony.kingdetective.service.ops;

import cn.hutool.core.util.IdUtil;
import com.tony.kingdetective.bean.params.ops.SshCredentialParams;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class WebSshSessionRegistry {
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();

    public Entry create(SshCredentialParams credential, int ttlMinutes) {
        cleanExpired();
        long ttlMillis = TimeUnit.MINUTES.toMillis(Math.max(1, Math.min(ttlMinutes, 120)));
        Entry entry = new Entry(IdUtil.fastSimpleUUID(), credential, System.currentTimeMillis() + ttlMillis);
        sessions.put(entry.sessionId(), entry);
        return entry;
    }

    public SshCredentialParams getCredential(String sessionId) {
        Entry entry = sessions.get(sessionId);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            sessions.remove(sessionId);
            return null;
        }
        return entry.credential();
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    public record Entry(String sessionId, SshCredentialParams credential, long expiresAt) {
    }
}
