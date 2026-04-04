package org.dispatchsystem.ai.ops;

import org.dispatchsystem.ai.dto.AiOpsSummaryResponse;
import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.ride.domain.Ride;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ZonePressureAnalyzer {
    public List<AiOpsSummaryResponse.ZonePressureDTO> findLowSupplyZones(List<RideDispatchEvent> events, List<Ride> rides) {
        Map<Long, Ride> ridesById = rides.stream()
                .collect(Collectors.toMap(Ride::getId, Function.identity(), (left, right) -> left));

        return events.stream()
                .filter(event -> event.getEventType() == EventType.DISPATCH_FAILED)
                .filter(event -> event.getNegativeReasons().contains(ReasonCode.NO_ELIGIBLE_DRIVERS)
                        || event.getNegativeReasons().contains(ReasonCode.ALL_OFFERS_EXHAUSTED))
                .map(RideDispatchEvent::getRide)
                .map(ride -> ridesById.getOrDefault(ride.getId(), ride))
                .collect(Collectors.groupingBy(this::zoneLabel, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(entry -> AiOpsSummaryResponse.ZonePressureDTO.builder()
                        .zoneLabel(entry.getKey())
                        .noSupplyCount(entry.getValue())
                        .build())
                .toList();
    }

    private String zoneLabel(Ride ride) {
        return String.format(Locale.ROOT, "%.2f, %.2f", roundCoordinate(ride.getStartLatitude()), roundCoordinate(ride.getStartLongitude()));
    }

    private double roundCoordinate(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
