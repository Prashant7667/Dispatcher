package org.dispatchsystem.ride.controller;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.ride.dto.DispatchAnalyticsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchTimelineEventDTO;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DispatchAdminController {
    private final DispatchAuditService dispatchAuditService;

    public DispatchAdminController(DispatchAuditService dispatchAuditService) {
        this.dispatchAuditService = dispatchAuditService;
    }

    @GetMapping("/rides/{rideId}/dispatch-timeline")
    public ResponseEntity<List<DispatchTimelineEventDTO>> getDispatchTimeline(@PathVariable Long rideId) {
        List<DispatchTimelineEventDTO> timeline = dispatchAuditService.getDispatchTimeline(rideId).stream()
                .map(this::toTimelineDto)
                .toList();
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/analytics/dispatch-summary")
    public ResponseEntity<DispatchAnalyticsSummaryDTO> getDispatchSummary() {
        return ResponseEntity.ok(dispatchAuditService.getDispatchAnalyticsSummary());
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
