package org.dispatchsystem.ride.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.service.DriverSessionRegistry;
import org.dispatchsystem.ride.domain.Ride;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Service
public class RideBroadcastService {

    private final DriverSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RideBroadcastService(DriverSessionRegistry registry) {
        this.registry = registry;
    }
    public void broadcastRidetoNearByDrivers(Ride ride, List<Driver> drivers){
        for(Driver driver:drivers){
            WebSocketSession session=registry.getAll().get(driver.getEmail());
            if(session==null || !session.isOpen())continue;
            try{
                String payload=objectMapper.writeValueAsString(ride);
                session.sendMessage(new TextMessage(payload));
            } catch (Exception e){
                registry.remove(driver.getEmail());
            }
        }

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
