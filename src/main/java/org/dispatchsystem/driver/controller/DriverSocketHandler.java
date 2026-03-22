package org.dispatchsystem.driver.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.dispatch.offer.DriverOfferResponse;
import org.dispatchsystem.dispatch.offer.DriverResponded;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.dispatch.offer.OfferStatusState;
import org.dispatchsystem.driver.service.DriverSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
@Component
public class DriverSocketHandler extends TextWebSocketHandler {

    private final DriverSessionRegistry driverSessionRegistry;
    private final OfferManager offerManager;
    private final ObjectMapper objectMapper;

    public DriverSocketHandler(DriverSessionRegistry driverSessionRegistry, OfferManager offerManager, ObjectMapper objectMapper) {
        this.driverSessionRegistry=driverSessionRegistry;
        this.offerManager = offerManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String role = (String) session.getAttributes().get("role");
        String email = (String) session.getAttributes().get("email");
        if ("DRIVER".equals(role)) {
            driverSessionRegistry.add(email, session);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage){
        try{
            String email = (String) session.getAttributes().get("email");
            String role= (String) session.getAttributes().get("role");
            String payload = textMessage.getPayload();
            if("DRIVER".equals(role)){
                DriverOfferResponse response = objectMapper.readValue(payload, DriverOfferResponse.class);
                DriverResponded driverResponded=offerManager.handleDriverResponse(response.getRideId(),email,response.getMessage());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(driverResponded)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            try{
                DriverResponded errorPayload=new DriverResponded(OfferStatusState.ERROR,null,"Unable to process driver response");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorPayload)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String role = (String) session.getAttributes().get("role");
        String email = (String) session.getAttributes().get("email");
        if ("DRIVER".equals(role)) {
            driverSessionRegistry.remove(email);
        }
    }
}
