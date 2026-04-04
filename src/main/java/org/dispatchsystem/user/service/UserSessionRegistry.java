package org.dispatchsystem.user.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class UserSessionRegistry {
    private final Map<String, WebSocketSession> users = new ConcurrentHashMap<>();

    public void add(String email, WebSocketSession session) {
        users.put(email, session);
    }

    public void remove(String email) {
        users.remove(email);
    }

    public Map<String, WebSocketSession> getAll() {
        return users;
    }
}
