package org.dispatchsystem.driver.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DriverSessionRegistry {

    private final Map<String, WebSocketSession> drivers = new ConcurrentHashMap<>();

    public void add(String email, WebSocketSession session) {
        System.out.println("Driver connected: " + email);
        drivers.put(email, session);
    }

    public void remove(String email) {
        drivers.remove(email);
    }

    public Map<String, WebSocketSession> getAll() {
        return drivers;
    }
}