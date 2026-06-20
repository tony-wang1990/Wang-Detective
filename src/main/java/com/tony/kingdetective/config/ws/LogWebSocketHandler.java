package com.tony.kingdetective.config.ws;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.Tailer;
import com.tony.kingdetective.service.AdminCredentialService;
import com.tony.kingdetective.utils.CommonUtils;
import com.tony.kingdetective.utils.TextEncodingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_RECENT_LOGS = 200;

    private final AdminCredentialService adminCredentialService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Deque<String> recentLogs = new ConcurrentLinkedDeque<>();
    private final Object tailerLock = new Object();
    private volatile Tailer tailer;

    public LogWebSocketHandler(AdminCredentialService adminCredentialService) {
        this.adminCredentialService = adminCredentialService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = getTokenFromSession(session);
        if (!adminCredentialService.verifyToken(token)) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("Invalid token"));
            return;
        }

        sessions.put(session.getId(), session);
        ensureTailerStarted();
        sendRecentLogs(session);
        log.debug("Log WebSocket connected: sessionId={}, activeSessions={}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        stopTailerWhenIdle();
        log.debug("Log WebSocket closed: sessionId={}, activeSessions={}", session.getId(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
        stopTailerWhenIdle();
        log.warn("Log WebSocket transport error: sessionId={}, message={}",
                session.getId(), exception.getMessage());
    }

    private void ensureTailerStarted() {
        if (tailer != null) {
            return;
        }
        synchronized (tailerLock) {
            if (tailer != null) {
                return;
            }
            File logFile = new File(CommonUtils.LOG_FILE_PATH);
            if (!logFile.exists()) {
                FileUtil.touch(logFile);
            }
            if (!logFile.isFile()) {
                throw new IllegalStateException("Invalid log file path: " + logFile);
            }

            Tailer newTailer = new Tailer(logFile, StandardCharsets.UTF_8, this::broadcastLine,
                    MAX_RECENT_LOGS, 1000);
            newTailer.start(true);
            tailer = newTailer;
        }
    }

    private void stopTailerWhenIdle() {
        if (!sessions.isEmpty()) {
            return;
        }
        synchronized (tailerLock) {
            if (!sessions.isEmpty() || tailer == null) {
                return;
            }
            tailer.stop();
            tailer = null;
        }
    }

    private void broadcastLine(String line) {
        String displayLine = TextEncodingUtils.repairMojibake(line);
        recentLogs.addLast(displayLine);
        while (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.pollFirst();
        }

        sessions.values().forEach(session -> send(session, displayLine));
    }

    private void sendRecentLogs(WebSocketSession session) {
        recentLogs.forEach(line -> send(session, line));
    }

    private void send(WebSocketSession session, String line) {
        if (session == null || !session.isOpen()) {
            if (session != null) {
                sessions.remove(session.getId());
            }
            return;
        }
        synchronized (session) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(line));
                }
            } catch (IOException e) {
                sessions.remove(session.getId());
                log.debug("Failed to push log WebSocket line: sessionId={}, message={}",
                        session.getId(), e.getMessage());
            }
        }
    }

    private String getTokenFromSession(WebSocketSession session) {
        if (session.getUri() == null || session.getUri().getRawQuery() == null) {
            return null;
        }
        for (String pair : session.getUri().getRawQuery().split("&")) {
            int splitIndex = pair.indexOf('=');
            if (splitIndex <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, splitIndex), StandardCharsets.UTF_8);
            if ("token".equals(key)) {
                return URLDecoder.decode(pair.substring(splitIndex + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException e) {
            log.debug("Failed to close log WebSocket session: {}", e.getMessage());
        }
    }
}
