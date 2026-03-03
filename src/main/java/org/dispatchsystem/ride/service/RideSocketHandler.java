package org.dispatchsystem.ride.service;

import org.dispatchsystem.driver.service.DriverSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RideSocketHandler extends TextWebSocketHandler {

    private final DriverSessionRegistry registry;

    public RideSocketHandler(DriverSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String role = (String) session.getAttributes().get("role");
        String email = (String) session.getAttributes().get("email");

        if ("DRIVER".equals(role)) {
            registry.add(email, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get("email");
        registry.remove(email);
    }
}
