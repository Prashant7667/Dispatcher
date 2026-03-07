package org.dispatchsystem.ride.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.dispatch.offer.DriverOfferResponse;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.driver.service.DriverSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RideSocketHandler extends TextWebSocketHandler {

    private final DriverSessionRegistry registry;
    private final OfferManager offerManager;
    private final ObjectMapper objectMapper;

    public RideSocketHandler(DriverSessionRegistry registry, OfferManager offerManager, ObjectMapper objectMapper) {
        this.registry = registry;
        this.offerManager = offerManager;
        this.objectMapper = objectMapper;
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
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage){
        try{
            String email = (String) session.getAttributes().get("email");
            String payload = textMessage.getPayload();
            DriverOfferResponse response = objectMapper.readValue(payload, DriverOfferResponse.class);
            offerManager.handleDriverResponse(response.getRideId(),email,response.getMessage());
        } catch (Exception e) {
           e.printStackTrace();
        }


    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get("email");
        registry.remove(email);
    }
}
