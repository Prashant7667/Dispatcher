package org.dispatchsystem.ai.explanations;

import org.dispatchsystem.common.events.domains.ReasonCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ReasonCodeNarrativeMapper {
    public String toPositivePhrase(ReasonCode reasonCode) {
        return switch (reasonCode) {
            case INSIDE_PICKUP_RADIUS -> "pickup was inside the allowed radius";
            case VEHICLE_CLASS_MATCHED -> "vehicle class matched";
            case LUGGAGE_CAPACITY_MATCHED -> "luggage capacity matched";
            case BOOKING_TYPE_SUPPORTED -> "booking type was supported";
            case SCHEDULE_AVAILABLE -> "driver was available for the requested schedule";
            case DURATION_SUPPORTED -> "ride duration was supported";
            case DRIVER_IS_AVAILABLE -> "driver was available";
            case DRIVER_ACCEPTED -> "driver accepted the offer";
            default -> toNegativePhrase(reasonCode);
        };
    }

    public String toNegativePhrase(ReasonCode reasonCode) {
        return switch (reasonCode) {
            case OUTSIDE_PICKUP_RADIUS -> "pickup distance was outside the allowed radius";
            case VEHICLE_CLASS_MISMATCH -> "vehicle class did not match the request";
            case LUGGAGE_CAPACITY_MISMATCH -> "luggage capacity was insufficient";
            case BOOKING_TYPE_UNSUPPORTED -> "booking type was not supported";
            case SCHEDULE_UNAVAILABLE -> "driver was unavailable for the scheduled time";
            case DURATION_UNSUPPORTED -> "ride duration was unsupported";
            case DRIVER_ALREADY_RESERVED -> "driver was already reserved";
            case NO_ELIGIBLE_DRIVERS -> "no eligible drivers were available";
            case PASSENGER_CANCELLED -> "the passenger cancelled the ride";
            case OFFER_TIMEOUT -> "the offer timed out";
            case DRIVER_REJECTED -> "the driver rejected the offer";
            case DRIVER_LOCATION_NOT_AVAILABLE -> "driver location was unavailable";
            case ALL_OFFERS_EXHAUSTED -> "all eligible offers were exhausted";
            default -> reasonCode.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    public String summarizePositiveReasons(List<ReasonCode> reasonCodes, String fallback) {
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            return normalizeFallback(fallback);
        }
        return reasonCodes.stream()
                .map(this::toPositivePhrase)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public String summarizeNegativeReasons(List<ReasonCode> reasonCodes, String fallback) {
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            return normalizeFallback(fallback);
        }
        return reasonCodes.stream()
                .map(this::toNegativePhrase)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String normalizeFallback(String fallback) {
        return fallback == null || fallback.isBlank() ? "no reason was recorded" : fallback;
    }
}
