package org.dispatchsystem.ai.ops;

import org.dispatchsystem.ai.dto.AiOpsSummaryRequest;
import org.dispatchsystem.ai.dto.AiOpsSummaryResponse;
import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.repository.RideDispatchEventRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OpsMetricAssembler {
    private final RideDispatchEventRepository rideDispatchEventRepository;
    private final RideRepository rideRepository;
    private final ZonePressureAnalyzer zonePressureAnalyzer;

    public OpsMetricAssembler(RideDispatchEventRepository rideDispatchEventRepository,
                              RideRepository rideRepository,
                              ZonePressureAnalyzer zonePressureAnalyzer) {
        this.rideDispatchEventRepository = rideDispatchEventRepository;
        this.rideRepository = rideRepository;
        this.zonePressureAnalyzer = zonePressureAnalyzer;
    }

    public AiOpsSummaryResponse assemble(AiOpsSummaryRequest request) {
        List<RideDispatchEvent> events = rideDispatchEventRepository.findByCreatedAtBetween(request.getFrom(), request.getTo());
        List<Ride> rides = rideRepository.findByCreatedAtBetween(request.getFrom(), request.getTo());

        long offersSent = countByEvent(events, EventType.OFFER_SENT);
        long offersAccepted = countByEvent(events, EventType.OFFER_ACCEPTED);

        return AiOpsSummaryResponse.builder()
                .from(request.getFrom())
                .to(request.getTo())
                .acceptanceRate(rate(offersAccepted, offersSent))
                .averageDispatchTimeSeconds(averageDispatchTimeSeconds(rides))
                .topCancellationReasons(topReasons(events, event -> event.getEventType() == EventType.RIDE_CANCELLED, 3))
                .topDispatchFailureReasons(topReasons(
                        events,
                        event -> event.getEventType() == EventType.DISPATCH_FAILED
                                || event.getEventType() == EventType.OFFER_REJECTED
                                || event.getEventType() == EventType.OFFER_TIMED_OUT,
                        5))
                .lowSupplyZones(zonePressureAnalyzer.findLowSupplyZones(events, rides))
                .build();
    }

    private long countByEvent(List<RideDispatchEvent> events, EventType eventType) {
        return events.stream().filter(event -> event.getEventType() == eventType).count();
    }

    private double averageDispatchTimeSeconds(List<Ride> rides) {
        double average = rides.stream()
                .filter(ride -> ride.getDispatchStartedAt() != null && ride.getDriverAssignedAt() != null)
                .mapToLong(ride -> Duration.between(ride.getDispatchStartedAt(), ride.getDriverAssignedAt()).getSeconds())
                .average()
                .orElse(0.0);
        return round(average);
    }

    private List<AiOpsSummaryResponse.ReasonCountDTO> topReasons(List<RideDispatchEvent> events,
                                                                 java.util.function.Predicate<RideDispatchEvent> filter,
                                                                 int limit) {
        return events.stream()
                .filter(filter)
                .flatMap(event -> event.getNegativeReasons().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<ReasonCode, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> AiOpsSummaryResponse.ReasonCountDTO.builder()
                        .reasonCode(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
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
