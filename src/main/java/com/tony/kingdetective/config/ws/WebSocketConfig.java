package com.tony.kingdetective.config.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.config
 * @className: WebSocketConfig
 * @author: Tony Wang
 * @date: 2024/11/17 18:35
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final LogWebSocketHandler logWebSocketHandler;
    private final SshTerminalWebSocketHandler sshTerminalWebSocketHandler;

    public WebSocketConfig(LogWebSocketHandler logWebSocketHandler,
                           SshTerminalWebSocketHandler sshTerminalWebSocketHandler) {
        this.logWebSocketHandler = logWebSocketHandler;
        this.sshTerminalWebSocketHandler = sshTerminalWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logWebSocketHandler, "/logs")
                .setAllowedOriginPatterns("*");
        registry.addHandler(sshTerminalWebSocketHandler, "/ops/ssh/terminal/{sessionId}")
                .setAllowedOriginPatterns("*");
    }

    @Bean
    @ConditionalOnProperty(prefix = "king-detective.websocket", name = "server-endpoint-exporter-enabled", havingValue = "true", matchIfMissing = true)
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
