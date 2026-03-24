package org.dispatchsystem.dispatch.geo;

import org.dispatchsystem.dispatch.DispatchCandidate;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.domain.VehicleClass;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.Ride;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
public class DriverEligibilityService {
    private final GeoService geoService;
    public DriverEligibilityService(GeoService geoService){
        this.geoService = geoService;
    }
    private boolean supportsVehicleClass(VehicleClass driverVehicleClass, VehicleClass rideVehicleClass) {
        return driverVehicleClass == rideVehicleClass;
    }
    private boolean supportsLuggageCapacity(int driverLuggageCapacity, int rideLuggageCapacity){
       return rideLuggageCapacity <= driverLuggageCapacity;
    }
    private boolean supportsBookingType(Driver driver, BookingType bookingType) {
        if (bookingType == null) {
            return true;
        }
        if (driver.getSupportedBookingTypes() == null || driver.getSupportedBookingTypes().isEmpty()) {
            return bookingType == BookingType.TRIP;
        }
        return driver.getSupportedBookingTypes().stream().anyMatch(type -> type.name().equals(bookingType.name()));
    }
    private boolean supportsDuration(Driver driver, Integer estimatedDurationMinutes) {
        if (estimatedDurationMinutes == null || driver.getMaxRentalDurationMinutes() == null) {
            return true;
        }
        return estimatedDurationMinutes <= driver.getMaxRentalDurationMinutes();
    }

    private boolean isAvailableForSchedule(Driver driver, LocalDateTime scheduledStart) {
        if (scheduledStart == null) {
            return true;
        }
        if (driver.getAvailableFrom() != null && scheduledStart.isBefore(driver.getAvailableFrom())) {
            return false;
        }
        if (driver.getAvailableUntil() != null && scheduledStart.isAfter(driver.getAvailableUntil())) {
            return false;
        }
        return true;
    }
    public DispatchCandidate evaluate(Ride ride, Driver driver){
        boolean bookingTypeFactor=supportsBookingType(driver,ride.getBookingType());
        boolean supportsDurationFactor=supportsDuration(driver, ride.getEstimatedDurationMinutes());
        boolean isAvailableForScheduleFactor=isAvailableForSchedule(driver, ride.getScheduledStart());
        boolean isVehicleClassAvailable = supportsVehicleClass(driver.getVehicleDetails().getVehicleClass(), ride.getRequestedVehicleClass());
        boolean isLuggageCapacityAvailable = supportsLuggageCapacity(driver.getVehicleDetails().getLuggageCapacityKg(), ride.getRequiredLuggageCapacity());
        List<String>AcceptedResponse = new ArrayList<>();
        List<String>RejectedResponse = new ArrayList<>();
        if(driver.getLongitude()==null||driver.getLatitude()==null){
            RejectedResponse.add("Driver coordinates are not available");
            return new DispatchCandidate(driver,0,false,AcceptedResponse, RejectedResponse, 0);
        }
        double distance= geoService.haversineDistance(driver.getLatitude(), driver.getLongitude(), ride.getStartLatitude(), ride.getStartLongitude());

        int score=0;
        if(bookingTypeFactor){
            AcceptedResponse.add("Booking type supported");
            score++;
        }
        if(!bookingTypeFactor){
            RejectedResponse.add("Booking type not supported");
        }
        if(supportsDurationFactor){
            AcceptedResponse.add("Duration supported");
            score++;
        }
        if(!supportsDurationFactor){
            RejectedResponse.add("Duration not supported");
        }
        if(isAvailableForScheduleFactor){
            AcceptedResponse.add("Schedule supported");
            score++;
        }
        if(!isAvailableForScheduleFactor){
            RejectedResponse.add("Schedule not supported");
        }
        if(distance<5){
            AcceptedResponse.add("Pickup distance is within 5 km");
            score++;
        }
        if(distance>=5){
            RejectedResponse.add("Pickup distance is more than 5 km");
        }
        if(isVehicleClassAvailable){
            AcceptedResponse.add("Requested vehicle class is available");
        }
        if(!isVehicleClassAvailable){
            RejectedResponse.add("Requested vehicle class is not available");
        }
        if(isLuggageCapacityAvailable){
            AcceptedResponse.add("Requested luggage capacity is available");
        }
        if(!isLuggageCapacityAvailable){
            RejectedResponse.add("Requested luggage capacity is not available");
        }
        boolean withinRadius = distance < 5;
        boolean eligible = bookingTypeFactor && supportsDurationFactor && isAvailableForScheduleFactor && withinRadius && isLuggageCapacityAvailable && isVehicleClassAvailable;

        return new DispatchCandidate(driver,distance,eligible,AcceptedResponse, RejectedResponse, score);
    }
    public List<DispatchCandidate> evaluateAll(Ride ride, List<Driver> drivers){
        List<DispatchCandidate>dispatchCandidateList=new ArrayList<>();
        for(Driver driver:drivers){
            dispatchCandidateList.add(evaluate(ride, driver));
        }
        return  dispatchCandidateList;
    }

}
