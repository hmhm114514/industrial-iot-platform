package com.practice.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeEventService extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void broadcast(String type, Map<String, Object> payload) {
        try {
            TextMessage message = new TextMessage(mapper.writeValueAsString(Map.of("type", type, "payload", payload)));
            sessions.removeIf((session) -> !session.isOpen());
            for (WebSocketSession session : sessions) session.sendMessage(message);
        } catch (Exception ex) {
            System.out.println("Realtime event broadcast failed: " + ex.getMessage());
        }
    }
}
