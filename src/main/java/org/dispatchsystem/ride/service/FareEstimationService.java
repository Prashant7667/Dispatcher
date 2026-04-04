package org.dispatchsystem.ride.service;

import org.dispatchsystem.dispatch.geo.GeoService;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.RentalPlan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class FareEstimationService {
    private static final Map<BookingType, Double> BASE_FARES = Map.of(
            BookingType.TRIP, 50.0,
            BookingType.HOURLY, 80.0,
            BookingType.DAILY, 300.0,
            BookingType.MONTHLY, 500.0
    );
    private static final Map<BookingType, Double> DISTANCE_RATES = Map.of(
            BookingType.TRIP, 12.0,
            BookingType.HOURLY, 10.0,
            BookingType.DAILY, 8.0,
            BookingType.MONTHLY, 6.0
    );
    private static final Map<BookingType, Double> MINUTE_RATES = Map.of(
            BookingType.TRIP, 2.0,
            BookingType.HOURLY, 3.0,
            BookingType.DAILY, 1.5,
            BookingType.MONTHLY, 1.0
    );
    private static final Map<RentalPlan, Double> RENTAL_PLAN_MULTIPLIERS = Map.of(
            RentalPlan.NONE, 1.0,
            RentalPlan.FLEXIBLE, 1.05,
            RentalPlan.WEEKEND, 1.10,
            RentalPlan.WEEKLY, 0.95,
            RentalPlan.MONTHLY_STANDARD, 0.90
    );

    private final GeoService geoService;

    public FareEstimationService(GeoService geoService){
        this.geoService=geoService;
    }

    public double fareEstimation(double startLatitude, double startLongitude, double endLatitude, double endLongitude,
                                 BookingType bookingType,
                                 Integer estimatedDurationMinutes,
                                 RentalPlan rentalPlan){
        BookingType normalizedBookingType = bookingType == null ? BookingType.TRIP : bookingType;
        RentalPlan normalizedRentalPlan = rentalPlan == null ? RentalPlan.NONE : rentalPlan;
        int normalizedDurationMinutes = estimatedDurationMinutes == null ? 0 : estimatedDurationMinutes;
        double distance = geoService.haversineDistance(startLatitude, startLongitude, endLatitude, endLongitude);

        double subtotal = BASE_FARES.get(normalizedBookingType)
                + (distance * DISTANCE_RATES.get(normalizedBookingType))
                + (normalizedDurationMinutes * MINUTE_RATES.get(normalizedBookingType));

        double total = subtotal * RENTAL_PLAN_MULTIPLIERS.get(normalizedRentalPlan);
        return BigDecimal.valueOf(total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
