package org.dispatchsystem.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.dto.DriverRideOfferNotification;
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
            DriverRideOfferNotification payload = DriverRideOfferNotification.builder()
                    .rideId(ride.getId())
                    .startLongitude(ride.getStartLongitude())
                    .startLatitude(ride.getStartLatitude())
                    .endLongitude(ride.getEndLongitude())
                    .endLatitude(ride.getEndLatitude())
                    .requestedVehicleClass(ride.getRequestedVehicleClass())
                    .requiredLuggageCapacity(ride.getRequiredLuggageCapacity())
                    .status(ride.getStatus())
                    .bookingType(ride.getBookingType())
                    .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                    .rentalPlan(ride.getRentalPlan())
                    .createdAt(ride.getCreatedAt())
                    .scheduledStart(ride.getScheduledStart())
                    .fare(ride.getFare())
                    .userId(ride.getUser() != null ? ride.getUser().getId() : null)
                    .userName(ride.getUser() != null ? ride.getUser().getName() : null)
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
        catch (Exception e){
            registry.remove(driver.getEmail());
        }
    }
}
