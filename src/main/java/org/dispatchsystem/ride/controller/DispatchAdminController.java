package org.dispatchsystem.ride.controller;

import org.dispatchsystem.ai.dto.AiDriverDecisionExplanationDTO;
import org.dispatchsystem.ai.dto.AiOpsSummaryResponse;
import org.dispatchsystem.ai.dto.AiRideExplanationResponse;
import org.dispatchsystem.ai.explanations.DispatchExplanationService;
import org.dispatchsystem.ai.ops.OpsInsightService;
import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.ride.dto.DispatchAnalyticsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchFailureReasonsResponseDTO;
import org.dispatchsystem.ride.dto.DispatchOpsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchRideExplanationDTO;
import org.dispatchsystem.ride.dto.DispatchTimelineEventDTO;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DispatchAdminController {
    private final DispatchAuditService dispatchAuditService;
    private final DispatchExplanationService dispatchExplanationService;
    private final OpsInsightService opsInsightService;

    public DispatchAdminController(DispatchAuditService dispatchAuditService,
                                   DispatchExplanationService dispatchExplanationService,
                                   OpsInsightService opsInsightService) {
        this.dispatchAuditService = dispatchAuditService;
        this.dispatchExplanationService = dispatchExplanationService;
        this.opsInsightService = opsInsightService;
    }

    @GetMapping("/rides/{rideId}/dispatch-timeline")
    public ResponseEntity<List<DispatchTimelineEventDTO>> getDispatchTimeline(@PathVariable Long rideId) {
        List<DispatchTimelineEventDTO> timeline = dispatchAuditService.getDispatchTimeline(rideId).stream()
                .map(this::toTimelineDto)
                .toList();
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/rides/{rideId}/dispatch-explanation")
    public ResponseEntity<DispatchRideExplanationDTO> getRideExplanation(@PathVariable Long rideId) {
        return ResponseEntity.ok(toDispatchRideExplanation(dispatchExplanationService.explainRide(rideId)));
    }

    @GetMapping("/analytics/dispatch-summary")
    public ResponseEntity<DispatchAnalyticsSummaryDTO> getDispatchSummary(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(dispatchAuditService.getDispatchAnalyticsSummary(from, to));
    }

    @GetMapping("/analytics/failure-reasons")
    public ResponseEntity<List<DispatchFailureReasonsResponseDTO>>getFailureReasonsCount(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to){
        return ResponseEntity.ok(dispatchAuditService.getDispatchFailureReasonsCount(from, to));
    }

    @GetMapping("/analytics/outcomes")
    public ResponseEntity<DispatchTimelineEventDTO.DispatchOutcomeBreakdownDTO>getOutcomeBreakdown(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to){
        return ResponseEntity.ok(dispatchAuditService.getOutcomeBreakdown(from, to));
    }

    @GetMapping("/analytics/ops-summary")
    public ResponseEntity<DispatchOpsSummaryDTO> getOpsSummary(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(toDispatchOpsSummary(opsInsightService.getOpsSummary(from, to)));
    }

    private DispatchTimelineEventDTO toTimelineDto(RideDispatchEvent event) {
        DispatchTimelineEventDTO dto = new DispatchTimelineEventDTO();
        dto.setEventId(event.getId());
        dto.setRideId(event.getRide().getId());
        dto.setEventType(event.getEventType());
        dto.setDispatchAttempt(event.getDispatchAttempt());
        dto.setDispatchRank(event.getDispatchRank());
        dto.setPickupDistanceKm(event.getPickupDistanceKm());
        dto.setCandidateScore(event.getCandidateScore());
        dto.setReasonDetails(event.getReasonDetails());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setPositiveReasons(event.getPositiveReasons());
        dto.setNegativeReasons(event.getNegativeReasons());
        if (event.getDriver() != null) {
            dto.setDriverId(event.getDriver().getId());
            dto.setDriverName(event.getDriver().getName());
        }
        return dto;
    }

    private DispatchRideExplanationDTO toDispatchRideExplanation(AiRideExplanationResponse response) {
        return DispatchRideExplanationDTO.builder()
                .rideId(response.getRideId())
                .finalStatus(response.getFinalStatus())
                .selectionExplanation(response.getSelectionExplanation())
                .failureExplanation(response.getFailureExplanation())
                .cancellationExplanation(response.getCancellationExplanation())
                .selectedDrivers(mapDriverDecisions(response.getSelectedDrivers()))
                .skippedDrivers(mapDriverDecisions(response.getSkippedDrivers()))
                .build();
    }

    private List<DispatchRideExplanationDTO.DriverDecisionDTO> mapDriverDecisions(List<AiDriverDecisionExplanationDTO> decisions) {
        if (decisions == null) {
            return List.of();
        }
        return decisions.stream()
                .map(decision -> DispatchRideExplanationDTO.DriverDecisionDTO.builder()
                        .driverId(decision.getDriverId())
                        .driverName(decision.getDriverName())
                        .dispatchAttempt(decision.getDispatchAttempt())
                        .dispatchRank(decision.getDispatchRank())
                        .pickupDistanceKm(decision.getPickupDistanceKm())
                        .candidateScore(decision.getCandidateScore())
                        .scoreBreakdown(toScoreBreakdown(decision))
                        .explanation(decision.getExplanation())
                        .positiveReasons(decision.getPositiveReasons())
                        .negativeReasons(decision.getNegativeReasons())
                        .build())
                .toList();
    }

    private DispatchRideExplanationDTO.ScoreBreakdownDTO toScoreBreakdown(AiDriverDecisionExplanationDTO decision) {
        if (decision.getScoreBreakdown() == null) {
            return null;
        }
        return DispatchRideExplanationDTO.ScoreBreakdownDTO.builder()
                .constraintsScore(decision.getScoreBreakdown().getConstraintsScore())
                .distanceScore(decision.getScoreBreakdown().getDistanceScore())
                .ratingScore(decision.getScoreBreakdown().getRatingScore())
                .build();
    }

    private DispatchOpsSummaryDTO toDispatchOpsSummary(AiOpsSummaryResponse response) {
        return DispatchOpsSummaryDTO.builder()
                .from(response.getFrom())
                .to(response.getTo())
                .acceptanceRate(response.getAcceptanceRate())
                .averageDispatchTimeSeconds(response.getAverageDispatchTimeSeconds())
                .topCancellationReasons(response.getTopCancellationReasons().stream()
                        .map(reason -> DispatchOpsSummaryDTO.ReasonCountDTO.builder()
                                .reasonCode(reason.getReasonCode())
                                .count(reason.getCount())
                                .build())
                        .toList())
                .topDispatchFailureReasons(response.getTopDispatchFailureReasons().stream()
                        .map(reason -> DispatchOpsSummaryDTO.ReasonCountDTO.builder()
                                .reasonCode(reason.getReasonCode())
                                .count(reason.getCount())
                                .build())
                        .toList())
                .lowSupplyZones(response.getLowSupplyZones().stream()
                        .map(zone -> DispatchOpsSummaryDTO.ZonePressureDTO.builder()
                                .zoneLabel(zone.getZoneLabel())
                                .noSupplyCount(zone.getNoSupplyCount())
                                .build())
                        .toList())
                .narrative(response.getNarrative())
                .build();
    }
}
