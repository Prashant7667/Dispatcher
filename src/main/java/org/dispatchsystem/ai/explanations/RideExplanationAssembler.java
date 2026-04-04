package org.dispatchsystem.ai.explanations;

import org.dispatchsystem.ai.dto.AiRideExplanationRequest;
import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.repository.RideDispatchEventRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RideExplanationAssembler {
    private final RideRepository rideRepository;
    private final RideDispatchEventRepository rideDispatchEventRepository;

    public RideExplanationAssembler(RideRepository rideRepository, RideDispatchEventRepository rideDispatchEventRepository) {
        this.rideRepository = rideRepository;
        this.rideDispatchEventRepository = rideDispatchEventRepository;
    }

    public AiRideExplanationRequest buildRideExplanationRequest(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        List<RideDispatchEvent> dispatchEvents = rideDispatchEventRepository.findByRide_IdOrderByCreatedAtAsc(rideId);

        return AiRideExplanationRequest.builder()
                .rideId(ride.getId())
                .finalStatus(ride.getStatus())
                .ride(toRideFacts(ride))
                .candidates(toCandidateFacts(dispatchEvents))
                .outcome(toOutcomeFacts(dispatchEvents, ride))
                .build();
    }

    private AiRideExplanationRequest.RideFacts toRideFacts(Ride ride) {
        return new AiRideExplanationRequest.RideFacts(
                ride.getBookingType(),
                ride.getRequestedVehicleClass(),
                ride.getRequiredLuggageCapacity(),
                ride.getEstimatedDurationMinutes(),
                ride.getScheduledStart(),
                ride.getStartLatitude(),
                ride.getStartLongitude(),
                ride.getDispatchStartedAt(),
                ride.getDriverAssignedAt(),
                ride.getCancelledAt()
        );
    }

    private List<AiRideExplanationRequest.CandidateFacts> toCandidateFacts(List<RideDispatchEvent> dispatchEvents) {
        Map<Long, List<RideDispatchEvent>> eventsByDriver = dispatchEvents.stream()
                .filter(event -> event.getDriver() != null)
                .collect(Collectors.groupingBy(event -> event.getDriver().getId(), LinkedHashMap::new, Collectors.toList()));

        List<AiRideExplanationRequest.CandidateFacts> candidates = new ArrayList<>();
        for (List<RideDispatchEvent> driverEvents : eventsByDriver.values()) {
            driverEvents.sort(Comparator.comparing(RideDispatchEvent::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

            RideDispatchEvent latestEvent = driverEvents.get(driverEvents.size() - 1);
            List<ReasonCode> acceptedReasons = driverEvents.stream()
                    .flatMap(event -> event.getPositiveReasons().stream())
                    .distinct()
                    .toList();
            List<ReasonCode> rejectedReasons = driverEvents.stream()
                    .flatMap(event -> event.getNegativeReasons().stream())
                    .distinct()
                    .toList();

            candidates.add(AiRideExplanationRequest.CandidateFacts.builder()
                    .driverId(latestEvent.getDriver().getId())
                    .driverName(latestEvent.getDriver().getName())
                    .dispatchAttempt(latestDispatchAttempt(driverEvents))
                    .dispatchRank(latestDispatchRank(driverEvents))
                    .eventType(latestEvent.getEventType())
                    .eligible(isEligible(driverEvents))
                    .pickupDistanceKm(latestPickupDistance(driverEvents))
                    .score(latestCandidateScore(driverEvents))
                    .scoreBreakdown(AiRideExplanationRequest.ScoreBreakdown.builder()
                            .constraintsScore(latestConstraintsScore(driverEvents))
                            .distanceScore(latestDistanceScore(driverEvents))
                            .ratingScore(latestRatingScore(driverEvents))
                            .build())
                    .acceptedReasons(acceptedReasons)
                    .rejectedReasons(rejectedReasons)
                    .build());
        }
        return candidates;
    }

    private AiRideExplanationRequest.OutcomeFacts toOutcomeFacts(List<RideDispatchEvent> dispatchEvents, Ride ride) {
        RideDispatchEvent finalEvent = dispatchEvents.stream()
                .filter(event -> event.getEventType() == EventType.DRIVER_ASSIGNED
                        || event.getEventType() == EventType.DISPATCH_FAILED
                        || event.getEventType() == EventType.RIDE_CANCELLED)
                .reduce((first, second) -> second)
                .orElse(null);

        return AiRideExplanationRequest.OutcomeFacts.builder()
                .finalEventType(finalEvent != null ? finalEvent.getEventType() : null)
                .assignedDriverId(resolveAssignedDriverId(finalEvent, ride))
                .assignedDriverName(resolveAssignedDriverName(finalEvent, ride))
                .finalReasonCodes(finalEvent != null ? finalEvent.getNegativeReasons() : List.of())
                .finalReasonDetails(finalEvent != null ? finalEvent.getReasonDetails() : null)
                .build();
    }

    private Integer latestDispatchAttempt(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream()
                .map(RideDispatchEvent::getDispatchAttempt)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Integer latestDispatchRank(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream()
                .map(RideDispatchEvent::getDispatchRank)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Double latestPickupDistance(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream()
                .map(RideDispatchEvent::getPickupDistanceKm)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Double latestCandidateScore(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream()
                .map(RideDispatchEvent::getCandidateScore)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Double latestConstraintsScore(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream()
                .map(RideDispatchEvent::getConstraintsScore)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Double latestDistanceScore(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream()
                .map(RideDispatchEvent::getDistanceScore)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Double latestRatingScore(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream()
                .map(RideDispatchEvent::getRatingScore)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Boolean isEligible(List<RideDispatchEvent> driverEvents) {
        return driverEvents.stream().anyMatch(event -> event.getEventType() == EventType.DRIVER_EVALUATED
                || event.getEventType() == EventType.OFFER_SENT
                || event.getEventType() == EventType.OFFER_ACCEPTED
                || event.getEventType() == EventType.OFFER_REJECTED
                || event.getEventType() == EventType.OFFER_TIMED_OUT
                || event.getEventType() == EventType.DRIVER_ASSIGNED);
    }

    private Long resolveAssignedDriverId(RideDispatchEvent finalEvent, Ride ride) {
        if (finalEvent != null && finalEvent.getDriver() != null) {
            return finalEvent.getDriver().getId();
        }
        return ride.getDriver() != null ? ride.getDriver().getId() : null;
    }

    private String resolveAssignedDriverName(RideDispatchEvent finalEvent, Ride ride) {
        if (finalEvent != null && finalEvent.getDriver() != null) {
            return finalEvent.getDriver().getName();
        }
        return ride.getDriver() != null ? ride.getDriver().getName() : null;
    }
}
