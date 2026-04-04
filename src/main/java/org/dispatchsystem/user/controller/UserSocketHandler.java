package org.dispatchsystem.user.controller;
import org.dispatchsystem.user.service.UserSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
@Component
public class UserSocketHandler extends TextWebSocketHandler {
    private final UserSessionRegistry userSessionRegistry;
    public UserSocketHandler( UserSessionRegistry userSessionRegistry) {
        this.userSessionRegistry=userSessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String role = (String) session.getAttributes().get("role");
        String email = (String) session.getAttributes().get("email");
       if ("USER".equals(role)) {
            userSessionRegistry.add(email,session);
       }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String role = (String) session.getAttributes().get("role");
        String email = (String) session.getAttributes().get("email");
        if ("USER".equals(role)) {
            userSessionRegistry.remove(email);
        }
    }
}

