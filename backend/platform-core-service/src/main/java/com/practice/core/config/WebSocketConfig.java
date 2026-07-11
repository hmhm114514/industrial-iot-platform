package com.practice.core.config;

import com.practice.core.service.RealtimeEventService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final RealtimeEventService realtimeEvents;

    public WebSocketConfig(RealtimeEventService realtimeEvents) {
        this.realtimeEvents = realtimeEvents;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeEvents, "/ws/events").setAllowedOrigins("*");
    }
}
