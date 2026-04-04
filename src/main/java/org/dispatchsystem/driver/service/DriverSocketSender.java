package org.dispatchsystem.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class DriverSocketSender {
    private final DriverSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public DriverSocketSender(DriverSessionRegistry driverSessionRegistry){
        this.registry=driverSessionRegistry;
    }
    public void sendOfferToDriver(Driver driver, Ride ride){
        WebSocketSession session=registry.getAll().get(driver.getEmail());
        if(session==null || !session.isOpen()){
            return;
        }
        try{
            String payload= objectMapper.writeValueAsString(ride);
            session.sendMessage(new TextMessage(payload));
        }
        catch (Exception e){
            registry.remove(driver.getEmail());
        }
    }
}
