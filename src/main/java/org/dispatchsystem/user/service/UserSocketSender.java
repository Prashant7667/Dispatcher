package org.dispatchsystem.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.user.domain.UserRideNotification;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class UserSocketSender {
    private final UserSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    UserSocketSender(UserSessionRegistry userSessionRegistry){
        this.registry=userSessionRegistry;
    }

    public void sendToUser(UserRideNotification notification){
        String email = notification.getRide().getUser().getEmail();
        WebSocketSession session=registry.getAll().get(email);
        if(session==null || !session.isOpen()){
            return;
        }
        try{
            String payload= objectMapper.writeValueAsString(notification);
            session.sendMessage(new TextMessage(payload));
        }
        catch (Exception e){
            registry.remove(email);
        }
    }
}
