package org.dispatchsystem.ride.service;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.common.exceptions.ResourceNotFoundException;
import org.dispatchsystem.dispatch.DispatchCandidate;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.dto.DispatchAnalyticsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchFailureReasonsResponseDTO;
import org.dispatchsystem.ride.dto.DispatchTimelineEventDTO;
import org.dispatchsystem.ride.repository.RideDispatchEventRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DispatchAuditService {
    private final RideDispatchEventRepository rideDispatchEventRepository;
    private final RideRepository rideRepository;

    public DispatchAuditService(RideDispatchEventRepository rideDispatchEventRepository, RideRepository rideRepository) {
        this.rideDispatchEventRepository = rideDispatchEventRepository;
        this.rideRepository = rideRepository;
    }

    public RideDispatchEvent recordDispatchStarted(Ride ride) {
        return saveEvent(ride, null, EventType.DISPATCH_STARTED, null, null, null, "Dispatch started");
    }

    public RideDispatchEvent recordDriverEvaluation(Ride ride, DispatchCandidate candidate) {
        EventType eventType = candidate.isEligible() ? EventType.DRIVER_EVALUATED : EventType.DRIVER_SKIPPED;
        String reasonDetails = candidate.isEligible()
                ? "Driver remained eligible after dispatch evaluation"
                : "Driver was skipped during dispatch evaluation";
        return saveEvent(
                ride,
                candidate.getDriver(),
                eventType,
                null,
                candidate.getAcceptedReasons(),
                candidate.getRejectedReasons(),
                reasonDetails
        );
    }

    public RideDispatchEvent recordOfferSent(Ride ride, Driver driver, int dispatchAttempt) {
        return saveEvent(ride, driver, EventType.OFFER_SENT, dispatchAttempt, null, null, "Offer sent to driver");
    }

    public RideDispatchEvent recordOfferAccepted(Ride ride, Driver driver, int dispatchAttempt) {
        return saveEvent(
                ride,
                driver,
                EventType.OFFER_ACCEPTED,
                dispatchAttempt,
                List.of(ReasonCode.DRIVER_ACCEPTED),
                null,
                "Driver accepted the dispatch offer"
        );
    }

    public RideDispatchEvent recordOfferRejected(Ride ride, Driver driver, int dispatchAttempt) {
        return saveEvent(
                ride,
                driver,
                EventType.OFFER_REJECTED,
                dispatchAttempt,
                null,
                List.of(ReasonCode.DRIVER_REJECTED),
                "Driver rejected the dispatch offer"
        );
    }

    public RideDispatchEvent recordOfferTimedOut(Ride ride, Driver driver, int dispatchAttempt) {
        return saveEvent(
                ride,
                driver,
                EventType.OFFER_TIMED_OUT,
                dispatchAttempt,
                null,
                List.of(ReasonCode.OFFER_TIMEOUT),
                "Driver offer timed out"
        );
    }

    public RideDispatchEvent recordDriverSkipped(Ride ride, Driver driver, int dispatchAttempt, ReasonCode reasonCode, String reasonDetails) {
        return saveEvent(ride, driver, EventType.DRIVER_SKIPPED, dispatchAttempt, null, List.of(reasonCode), reasonDetails);
    }

    public RideDispatchEvent recordDriverAssigned(Ride ride, Driver driver, int dispatchAttempt) {
        return saveEvent(
                ride,
                driver,
                EventType.DRIVER_ASSIGNED,
                dispatchAttempt,
                List.of(ReasonCode.DRIVER_ACCEPTED),
                null,
                "Driver assigned to ride"
        );
    }
    public List<DispatchFailureReasonsResponseDTO> getDispatchFailureReasonsCount(LocalDateTime from, LocalDateTime to) {
        List<RideDispatchEvent> rideDispatchEvents=rideDispatchEventRepository.findByCreatedAtBetween(from, to);
        return rideDispatchEvents.stream()
                .filter(event -> event.getEventType() == EventType.DISPATCH_FAILED
                        || event.getEventType() == EventType.RIDE_CANCELLED
                        || event.getEventType() == EventType.OFFER_REJECTED
                        || event.getEventType() == EventType.OFFER_TIMED_OUT)
                .flatMap(event -> event.getNegativeReasons().stream())
                .collect(Collectors.groupingBy(reason -> reason, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new DispatchFailureReasonsResponseDTO(entry.getKey(), entry.getValue()))
                .toList();
    }
    public DispatchTimelineEventDTO.DispatchOutcomeBreakdownDTO getOutcomeBreakdown(LocalDateTime from, LocalDateTime to) {
        List<RideDispatchEvent> rideDispatchEvents =
                rideDispatchEventRepository.findByCreatedAtBetween(from, to);

        long successfulAssignments = rideDispatchEvents.stream()
                .filter(event -> event.getEventType() == EventType.DRIVER_ASSIGNED)
                .count();

        Map<ReasonCode, Long> reasonCounts = rideDispatchEvents.stream()
                .filter(event -> event.getEventType() == EventType.DISPATCH_FAILED
                        || event.getEventType() == EventType.RIDE_CANCELLED
                        || event.getEventType() == EventType.OFFER_REJECTED
                        || event.getEventType() == EventType.OFFER_TIMED_OUT)
                .flatMap(event -> event.getNegativeReasons().stream())
                .collect(Collectors.groupingBy(reason -> reason, Collectors.counting()));

        return DispatchTimelineEventDTO.DispatchOutcomeBreakdownDTO.builder()
                .successfulAssignments(successfulAssignments)
                .failedNoEligibleDrivers(reasonCounts.getOrDefault(ReasonCode.NO_ELIGIBLE_DRIVERS, 0L))
                .failedAllOffersExhausted(reasonCounts.getOrDefault(ReasonCode.ALL_OFFERS_EXHAUSTED, 0L))
                .cancelledByPassenger(reasonCounts.getOrDefault(ReasonCode.PASSENGER_CANCELLED, 0L))
                .offerRejectedCount(reasonCounts.getOrDefault(ReasonCode.DRIVER_REJECTED, 0L))
                .offerTimeoutCount(reasonCounts.getOrDefault(ReasonCode.OFFER_TIMEOUT, 0L))
                .build();
    }




    public RideDispatchEvent recordDispatchFailed(Ride ride, ReasonCode reasonCode, String reasonDetails) {
        return saveEvent(ride, null, EventType.DISPATCH_FAILED, null, null, List.of(reasonCode), reasonDetails);
    }

    public RideDispatchEvent recordRideCancelled(Ride ride, Driver driver, Integer dispatchAttempt, ReasonCode reasonCode, String reasonDetails) {
        return saveEvent(ride, driver, EventType.RIDE_CANCELLED, dispatchAttempt, null, List.of(reasonCode), reasonDetails);
    }

    public List<RideDispatchEvent> getDispatchTimeline(Long rideId) {
        if (!rideRepository.existsById(rideId)) {
            throw new ResourceNotFoundException("Ride not found with id: " + rideId);
        }
        return rideDispatchEventRepository.findByRide_IdOrderByCreatedAtAsc(rideId);
    }

    public DispatchAnalyticsSummaryDTO getDispatchAnalyticsSummary(LocalDateTime from, LocalDateTime to) {
        List<RideDispatchEvent> auditEvents = rideDispatchEventRepository.findByCreatedAtBetween(from, to);
        List<Ride> rides = rideRepository.findByCreatedAtBetween(from , to);

        long totalOffersSent = auditEvents.stream().filter(event -> event.getEventType() == EventType.OFFER_SENT).count();
        long acceptedOffers = auditEvents.stream().filter(event -> event.getEventType() == EventType.OFFER_ACCEPTED).count();
        long rejectedOffers = auditEvents.stream().filter(event -> event.getEventType() == EventType.OFFER_REJECTED).count();
        long timedOutOffers = auditEvents.stream().filter(event -> event.getEventType() == EventType.OFFER_TIMED_OUT).count();
        long cancelledRides = rides.stream().filter(ride -> ride.getStatus() == RideStatus.CANCELLED).count();

        double averageDispatchTimeSeconds = rides.stream()
                .filter(ride -> ride.getDispatchStartedAt() != null && ride.getDriverAssignedAt() != null)
                .mapToLong(ride -> Duration.between(ride.getDispatchStartedAt(), ride.getDriverAssignedAt()).getSeconds())
                .average()
                .orElse(0.0);

        long utilizedDrivers = rides.stream()
                .filter(ride -> ride.getDriver() != null)
                .map(ride -> ride.getDriver().getId())
                .distinct()
                .count();
        long totalDriversSeen = auditEvents.stream()
                .filter(event -> event.getDriver() != null)
                .map(event -> event.getDriver().getId())
                .distinct()
                .count();

        return DispatchAnalyticsSummaryDTO.builder()
                .totalRides(rides.size())
                .totalOffersSent(totalOffersSent)
                .acceptedOffers(acceptedOffers)
                .rejectedOffers(rejectedOffers)
                .timedOutOffers(timedOutOffers)
                .cancelledRides(cancelledRides)
                .acceptanceRate(rate(acceptedOffers, totalOffersSent))
                .rejectionRate(rate(rejectedOffers, totalOffersSent))
                .timeoutRate(rate(timedOutOffers, totalOffersSent))
                .cancellationRate(rate(cancelledRides, rides.size()))
                .averageDispatchTimeSeconds(round(averageDispatchTimeSeconds))
                .driverUtilizationRate(rate(utilizedDrivers, totalDriversSeen))
                .build();
    }

    private RideDispatchEvent saveEvent(
            Ride ride,
            Driver driver,
            EventType eventType,
            Integer dispatchAttempt,
            List<ReasonCode> positiveReasons,
            List<ReasonCode> negativeReasons,
            String reasonDetails
    ) {
        RideDispatchEvent event = RideDispatchEvent.builder()
                .ride(ride)
                .driver(driver)
                .eventType(eventType)
                .dispatchAttempt(dispatchAttempt)
                .reasonDetails(reasonDetails)
                .createdAt(LocalDateTime.now())
                .positiveReasons(positiveReasons == null ? List.of() : List.copyOf(positiveReasons))
                .negativeReasons(negativeReasons == null ? List.of() : List.copyOf(negativeReasons))
                .build();
        return rideDispatchEventRepository.save(event);
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return round(((double) numerator / denominator) * 100.0);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
