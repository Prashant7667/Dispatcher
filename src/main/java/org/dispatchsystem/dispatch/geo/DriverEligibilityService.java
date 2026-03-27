package org.dispatchsystem.dispatch.geo;

import org.dispatchsystem.common.events.domains.ReasonCode;
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
        List<ReasonCode>AcceptedResponse = new ArrayList<>();
        List<ReasonCode>RejectedResponse = new ArrayList<>();
        if(driver.getLongitude()==null||driver.getLatitude()==null){
            RejectedResponse.add(ReasonCode.DRIVER_LOCATION_NOT_AVAILABLE);
            return new DispatchCandidate(driver,0,false,AcceptedResponse, RejectedResponse, 0);
        }
        double distance= geoService.haversineDistance(driver.getLatitude(), driver.getLongitude(), ride.getStartLatitude(), ride.getStartLongitude());

        int score=0;
        if(bookingTypeFactor){
            AcceptedResponse.add(ReasonCode.BOOKING_TYPE_SUPPORTED);
            score++;
        }
        if(!bookingTypeFactor){
            RejectedResponse.add(ReasonCode.BOOKING_TYPE_UNSUPPORTED);
        }
        if(supportsDurationFactor){
            AcceptedResponse.add(ReasonCode.DURATION_SUPPORTED);
            score++;
        }
        if(!supportsDurationFactor){
            RejectedResponse.add(ReasonCode.DURATION_UNSUPPORTED);
        }
        if(isAvailableForScheduleFactor){
            AcceptedResponse.add(ReasonCode.SCHEDULE_AVAILABLE);
            score++;
        }
        if(!isAvailableForScheduleFactor){
            RejectedResponse.add(ReasonCode.SCHEDULE_UNAVAILABLE);
        }
        if(distance<5){
            AcceptedResponse.add(ReasonCode.INSIDE_PICKUP_RADIUS);
            score++;
        }
        if(distance>=5){
            RejectedResponse.add(ReasonCode.OUTSIDE_PICKUP_RADIUS);
        }
        if(isVehicleClassAvailable){
            AcceptedResponse.add(ReasonCode.VEHICLE_CLASS_MATCHED);
        }
        if(!isVehicleClassAvailable){
            RejectedResponse.add(ReasonCode.VEHICLE_CLASS_MISMATCH);
        }
        if(isLuggageCapacityAvailable){
            AcceptedResponse.add(ReasonCode.LUGGAGE_CAPACITY_MATCHED);
        }
        if(!isLuggageCapacityAvailable){
            RejectedResponse.add(ReasonCode.LUGGAGE_CAPACITY_MISMATCH);
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
