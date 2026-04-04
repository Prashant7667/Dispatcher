package org.dispatchsystem.ride.controller;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.ride.dto.DispatchAnalyticsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchFailureReasonsResponseDTO;
import org.dispatchsystem.ride.dto.DispatchOpsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchRideExplanationDTO;
import org.dispatchsystem.ride.dto.DispatchTimelineEventDTO;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.dispatchsystem.ride.service.DispatchInsightService;
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
    private final DispatchInsightService dispatchInsightService;

    public DispatchAdminController(DispatchAuditService dispatchAuditService, DispatchInsightService dispatchInsightService) {
        this.dispatchAuditService = dispatchAuditService;
        this.dispatchInsightService = dispatchInsightService;
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
        return ResponseEntity.ok(dispatchInsightService.getRideExplanation(rideId));
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
        return ResponseEntity.ok(dispatchInsightService.getOpsSummary(from, to));
    }

    private DispatchTimelineEventDTO toTimelineDto(RideDispatchEvent event) {
        DispatchTimelineEventDTO dto = new DispatchTimelineEventDTO();
        dto.setEventId(event.getId());
        dto.setRideId(event.getRide().getId());
        dto.setEventType(event.getEventType());
        dto.setDispatchAttempt(event.getDispatchAttempt());
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
}
