package org.dispatchsystem.dispatch.geo;

import org.dispatchsystem.dispatch.DispatchCandidate;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.domain.DriverBookingType;
import org.dispatchsystem.driver.domain.VehicleClass;
import org.dispatchsystem.driver.domain.VehicleDetails;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.Ride;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DriverEligibilityServiceTest {

    private final DriverEligibilityService driverEligibilityService =
            new DriverEligibilityService(new GeoService(mock(DriverRepository.class)));

    @Test
    void evaluateRejectsDriverWithoutCoordinates() {
        Ride ride = createRide(VehicleClass.SEDAN, 10);
        Driver driver = createDriver(VehicleClass.SEDAN, 20);
        driver.setLatitude(null);

        DispatchCandidate candidate = driverEligibilityService.evaluate(ride, driver);

        assertFalse(candidate.isEligible());
        assertTrue(candidate.getRejectedReasons().contains("Driver coordinates are not available"));
    }

    @Test
    void evaluateRejectsDriverWithMismatchedVehicleClass() {
        Ride ride = createRide(VehicleClass.SEDAN, 10);
        Driver driver = createDriver(VehicleClass.SUV, 20);

        DispatchCandidate candidate = driverEligibilityService.evaluate(ride, driver);

        assertFalse(candidate.isEligible());
        assertTrue(candidate.getRejectedReasons().contains("Requested vehicle class is not available"));
    }

    @Test
    void evaluateRejectsDriverWithInsufficientLuggageCapacity() {
        Ride ride = createRide(VehicleClass.SEDAN, 25);
        Driver driver = createDriver(VehicleClass.SEDAN, 15);

        DispatchCandidate candidate = driverEligibilityService.evaluate(ride, driver);

        assertFalse(candidate.isEligible());
        assertTrue(candidate.getRejectedReasons().contains("Requested luggage capacity is not available"));
    }

    @Test
    void evaluateAcceptsEligibleDriver() {
        Ride ride = createRide(VehicleClass.SEDAN, 15);
        Driver driver = createDriver(VehicleClass.SEDAN, 25);

        DispatchCandidate candidate = driverEligibilityService.evaluate(ride, driver);

        assertTrue(candidate.isEligible());
        assertTrue(candidate.getAcceptedReasons().contains("Requested vehicle class is available"));
        assertTrue(candidate.getAcceptedReasons().contains("Requested luggage capacity is available"));
    }

    private Ride createRide(VehicleClass requestedVehicleClass, int requiredLuggageCapacity) {
        Ride ride = new Ride();
        ride.setStartLatitude(12.9716);
        ride.setStartLongitude(77.5946);
        ride.setBookingType(BookingType.TRIP);
        ride.setRequestedVehicleClass(requestedVehicleClass);
        ride.setRequiredLuggageCapacity(requiredLuggageCapacity);
        return ride;
    }

    private Driver createDriver(VehicleClass vehicleClass, int luggageCapacityKg) {
        Driver driver = new Driver();
        driver.setLatitude(12.9720);
        driver.setLongitude(77.5950);
        driver.setSupportedBookingTypes(Set.of(DriverBookingType.TRIP));

        VehicleDetails vehicleDetails = new VehicleDetails();
        vehicleDetails.setVehicleClass(vehicleClass);
        vehicleDetails.setLuggageCapacityKg(luggageCapacityKg);
        vehicleDetails.setSeatCapacity(4);
        vehicleDetails.setCity("Bengaluru");
        vehicleDetails.setZone("South");
        vehicleDetails.setVehicleMake("Hyundai");
        vehicleDetails.setVehicleModel("Verna");
        vehicleDetails.setVehicleColor("White");
        vehicleDetails.setLicensePlate("KA01AB1234");
        vehicleDetails.setVehicleYear(2024);
        driver.setVehicleDetails(vehicleDetails);
        return driver;
    }
}
