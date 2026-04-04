package org.dispatchsystem.ride.service;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.dto.DispatchOpsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchRideExplanationDTO;
import org.dispatchsystem.ride.repository.RideDispatchEventRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DispatchInsightService {
    private final RideDispatchEventRepository rideDispatchEventRepository;
    private final RideRepository rideRepository;

    public DispatchInsightService(RideDispatchEventRepository rideDispatchEventRepository, RideRepository rideRepository) {
        this.rideDispatchEventRepository = rideDispatchEventRepository;
        this.rideRepository = rideRepository;
    }

    public DispatchRideExplanationDTO getRideExplanation(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        List<RideDispatchEvent> events = rideDispatchEventRepository.findByRide_IdOrderByCreatedAtAsc(rideId);

        List<DispatchRideExplanationDTO.DriverDecisionDTO> selectedDrivers = events.stream()
                .filter(event -> event.getEventType() == EventType.DRIVER_ASSIGNED
                        || event.getEventType() == EventType.OFFER_ACCEPTED)
                .filter(event -> event.getDriver() != null)
                .map(this::toDriverDecision)
                .toList();

        List<DispatchRideExplanationDTO.DriverDecisionDTO> skippedDrivers = events.stream()
                .filter(event -> event.getEventType() == EventType.DRIVER_SKIPPED
                        || event.getEventType() == EventType.OFFER_REJECTED
                        || event.getEventType() == EventType.OFFER_TIMED_OUT)
                .filter(event -> event.getDriver() != null)
                .map(this::toDriverDecision)
                .toList();

        String selectionExplanation = buildSelectionExplanation(selectedDrivers, skippedDrivers);
        String failureExplanation = buildOutcomeExplanation(events, EventType.DISPATCH_FAILED, "Ride dispatch failed because ");
        String cancellationExplanation = buildOutcomeExplanation(events, EventType.RIDE_CANCELLED, "Ride was cancelled because ");

        return DispatchRideExplanationDTO.builder()
                .rideId(ride.getId())
                .finalStatus(ride.getStatus())
                .selectionExplanation(selectionExplanation)
                .failureExplanation(failureExplanation)
                .cancellationExplanation(cancellationExplanation)
                .selectedDrivers(selectedDrivers)
                .skippedDrivers(skippedDrivers)
                .build();
    }

    public DispatchOpsSummaryDTO getOpsSummary(LocalDateTime from, LocalDateTime to) {
        List<RideDispatchEvent> events = rideDispatchEventRepository.findByCreatedAtBetween(from, to);
        List<Ride> rides = rideRepository.findByCreatedAtBetween(from, to);

        long offersSent = countByEvent(events, EventType.OFFER_SENT);
        long offersAccepted = countByEvent(events, EventType.OFFER_ACCEPTED);

        List<DispatchOpsSummaryDTO.ReasonCountDTO> topCancellationReasons = topReasons(
                events,
                event -> event.getEventType() == EventType.RIDE_CANCELLED,
                3
        );
        List<DispatchOpsSummaryDTO.ReasonCountDTO> topFailureReasons = topReasons(
                events,
                event -> event.getEventType() == EventType.DISPATCH_FAILED
                        || event.getEventType() == EventType.OFFER_REJECTED
                        || event.getEventType() == EventType.OFFER_TIMED_OUT,
                5
        );

        Map<Long, Ride> ridesById = rides.stream()
                .collect(Collectors.toMap(Ride::getId, Function.identity(), (left, right) -> left));
        List<DispatchOpsSummaryDTO.ZonePressureDTO> lowSupplyZones = events.stream()
                .filter(event -> event.getEventType() == EventType.DISPATCH_FAILED)
                .filter(event -> event.getNegativeReasons().contains(ReasonCode.NO_ELIGIBLE_DRIVERS)
                        || event.getNegativeReasons().contains(ReasonCode.ALL_OFFERS_EXHAUSTED))
                .map(RideDispatchEvent::getRide)
                .map(ride -> ridesById.getOrDefault(ride.getId(), ride))
                .collect(Collectors.groupingBy(this::zoneLabel, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(entry -> DispatchOpsSummaryDTO.ZonePressureDTO.builder()
                        .zoneLabel(entry.getKey())
                        .noSupplyCount(entry.getValue())
                        .build())
                .toList();

        double averageDispatchTimeSeconds = rides.stream()
                .filter(ride -> ride.getDispatchStartedAt() != null && ride.getDriverAssignedAt() != null)
                .mapToLong(ride -> Duration.between(ride.getDispatchStartedAt(), ride.getDriverAssignedAt()).getSeconds())
                .average()
                .orElse(0.0);

        double acceptanceRate = rate(offersAccepted, offersSent);
        String narrative = buildOpsNarrative(acceptanceRate, topFailureReasons, topCancellationReasons, lowSupplyZones);

        return DispatchOpsSummaryDTO.builder()
                .from(from)
                .to(to)
                .acceptanceRate(acceptanceRate)
                .averageDispatchTimeSeconds(round(averageDispatchTimeSeconds))
                .topCancellationReasons(topCancellationReasons)
                .topDispatchFailureReasons(topFailureReasons)
                .lowSupplyZones(lowSupplyZones)
                .narrative(narrative)
                .build();
    }

    private DispatchRideExplanationDTO.DriverDecisionDTO toDriverDecision(RideDispatchEvent event) {
        return DispatchRideExplanationDTO.DriverDecisionDTO.builder()
                .driverId(event.getDriver().getId())
                .driverName(event.getDriver().getName())
                .dispatchAttempt(event.getDispatchAttempt())
                .explanation(explainEvent(event))
                .positiveReasons(event.getPositiveReasons())
                .negativeReasons(event.getNegativeReasons())
                .build();
    }

    private String buildSelectionExplanation(List<DispatchRideExplanationDTO.DriverDecisionDTO> selectedDrivers,
                                             List<DispatchRideExplanationDTO.DriverDecisionDTO> skippedDrivers) {
        if (selectedDrivers.isEmpty()) {
            return "No driver was selected for this ride.";
        }
        DispatchRideExplanationDTO.DriverDecisionDTO selectedDriver = selectedDrivers.get(selectedDrivers.size() - 1);
        if (skippedDrivers.isEmpty()) {
            return selectedDriver.getDriverName() + " was selected because they satisfied the ride constraints and accepted the first offer.";
        }
        return selectedDriver.getDriverName() + " was selected after earlier candidates were skipped, rejected, or timed out. "
                + "Their recorded evaluation and offer outcome remained the best successful path for the ride.";
    }

    private String buildOutcomeExplanation(List<RideDispatchEvent> events, EventType eventType, String prefix) {
        return events.stream()
                .filter(event -> event.getEventType() == eventType)
                .reduce((first, second) -> second)
                .map(event -> prefix + explainReasons(event.getNegativeReasons(), event.getReasonDetails()))
                .orElse(null);
    }

    private String explainEvent(RideDispatchEvent event) {
        if (event.getEventType() == EventType.DRIVER_ASSIGNED) {
            return event.getDriver().getName() + " was assigned after accepting the offer.";
        }
        if (event.getEventType() == EventType.OFFER_ACCEPTED) {
            return event.getDriver().getName() + " accepted the offer on attempt " + event.getDispatchAttempt() + ".";
        }
        if (event.getEventType() == EventType.OFFER_REJECTED) {
            return event.getDriver().getName() + " was skipped after rejecting the offer.";
        }
        if (event.getEventType() == EventType.OFFER_TIMED_OUT) {
            return event.getDriver().getName() + " was skipped because the offer timed out.";
        }
        if (event.getEventType() == EventType.DRIVER_SKIPPED) {
            return event.getDriver().getName() + " was skipped because " + explainReasons(event.getNegativeReasons(), event.getReasonDetails()) + ".";
        }
        return event.getReasonDetails();
    }

    private String explainReasons(List<ReasonCode> reasonCodes, String fallback) {
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            return fallback == null ? "no reason was recorded" : fallback.toLowerCase(Locale.ROOT);
        }
        return reasonCodes.stream()
                .map(this::toPhrase)
                .collect(Collectors.joining(", "));
    }

    private List<DispatchOpsSummaryDTO.ReasonCountDTO> topReasons(List<RideDispatchEvent> events,
                                                                  java.util.function.Predicate<RideDispatchEvent> filter,
                                                                  int limit) {
        return events.stream()
                .filter(filter)
                .flatMap(event -> event.getNegativeReasons().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<ReasonCode, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> DispatchOpsSummaryDTO.ReasonCountDTO.builder()
                        .reasonCode(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private String buildOpsNarrative(double acceptanceRate,
                                     List<DispatchOpsSummaryDTO.ReasonCountDTO> topFailureReasons,
                                     List<DispatchOpsSummaryDTO.ReasonCountDTO> topCancellationReasons,
                                     List<DispatchOpsSummaryDTO.ZonePressureDTO> lowSupplyZones) {
        String topFailure = topFailureReasons.isEmpty()
                ? "No major dispatch failure reason stood out."
                : "Top dispatch issue was " + toPhrase(topFailureReasons.get(0).getReasonCode()) + ".";
        String topCancellation = topCancellationReasons.isEmpty()
                ? "No cancellation pattern stood out."
                : "Top cancellation reason was " + toPhrase(topCancellationReasons.get(0).getReasonCode()) + ".";
        String lowSupply = lowSupplyZones.isEmpty()
                ? "No low-supply hotspot was detected."
                : "Lowest supply pressure appeared around " + lowSupplyZones.get(0).getZoneLabel() + ".";
        return "Acceptance rate was " + round(acceptanceRate) + "%. " + topFailure + " " + topCancellation + " " + lowSupply;
    }

    private long countByEvent(List<RideDispatchEvent> events, EventType eventType) {
        return events.stream()
                .filter(event -> event.getEventType() == eventType)
                .count();
    }

    private String zoneLabel(Ride ride) {
        return String.format(Locale.ROOT, "%.2f, %.2f", roundCoordinate(ride.getStartLatitude()), roundCoordinate(ride.getStartLongitude()));
    }

    private double roundCoordinate(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String toPhrase(ReasonCode reasonCode) {
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
            case INSIDE_PICKUP_RADIUS -> "pickup was inside the allowed radius";
            case VEHICLE_CLASS_MATCHED -> "vehicle class matched";
            case LUGGAGE_CAPACITY_MATCHED -> "luggage capacity matched";
            case BOOKING_TYPE_SUPPORTED -> "booking type was supported";
            case SCHEDULE_AVAILABLE -> "driver was available for the requested schedule";
            case DURATION_SUPPORTED -> "ride duration was supported";
            case DRIVER_IS_AVAILABLE -> "driver was available";
            case DRIVER_ACCEPTED -> "driver accepted the offer";
            case ALL_OFFERS_EXHAUSTED -> "all eligible offers were exhausted";
        };
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return round(((double) numerator / denominator) * 100.0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
