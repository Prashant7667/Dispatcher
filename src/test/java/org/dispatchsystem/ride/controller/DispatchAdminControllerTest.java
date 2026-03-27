package org.dispatchsystem.ride.controller;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.common.exceptions.GlobalExceptionHandler;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.dto.DispatchAnalyticsSummaryDTO;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DispatchAdminControllerTest {

    private MockMvc mockMvc;
    private DispatchAuditService dispatchAuditService;

    @BeforeEach
    void setUp() {
        dispatchAuditService = mock(DispatchAuditService.class);
        DispatchAdminController controller = new DispatchAdminController(dispatchAuditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDispatchTimelineReturnsMappedAuditEvents() throws Exception {
        Ride ride = new Ride();
        ride.setId(44L);

        Driver driver = new Driver();
        driver.setId(9L);
        driver.setName("Nisha");

        RideDispatchEvent event = RideDispatchEvent.builder()
                .id(101L)
                .ride(ride)
                .driver(driver)
                .eventType(EventType.OFFER_REJECTED)
                .dispatchAttempt(2)
                .reasonDetails("Driver rejected the dispatch offer")
                .createdAt(LocalDateTime.of(2026, 3, 27, 10, 30))
                .positiveReasons(List.of())
                .negativeReasons(List.of(ReasonCode.DRIVER_REJECTED))
                .build();

        when(dispatchAuditService.getDispatchTimeline(44L)).thenReturn(List.of(event));

        mockMvc.perform(get("/admin/rides/44/dispatch-timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(101))
                .andExpect(jsonPath("$[0].rideId").value(44))
                .andExpect(jsonPath("$[0].driverId").value(9))
                .andExpect(jsonPath("$[0].driverName").value("Nisha"))
                .andExpect(jsonPath("$[0].eventType").value("OFFER_REJECTED"))
                .andExpect(jsonPath("$[0].dispatchAttempt").value(2))
                .andExpect(jsonPath("$[0].negativeReasons[0]").value("DRIVER_REJECTED"));
    }

    @Test
    void getDispatchSummaryReturnsAnalyticsSnapshot() throws Exception {
        DispatchAnalyticsSummaryDTO summary = DispatchAnalyticsSummaryDTO.builder()
                .totalRides(12)
                .totalOffersSent(8)
                .acceptedOffers(3)
                .rejectedOffers(2)
                .timedOutOffers(3)
                .cancelledRides(4)
                .acceptanceRate(37.5)
                .rejectionRate(25.0)
                .timeoutRate(37.5)
                .cancellationRate(33.33)
                .averageDispatchTimeSeconds(48.25)
                .driverUtilizationRate(66.67)
                .build();

        when(dispatchAuditService.getDispatchAnalyticsSummary()).thenReturn(summary);

        mockMvc.perform(get("/admin/analytics/dispatch-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRides").value(12))
                .andExpect(jsonPath("$.acceptedOffers").value(3))
                .andExpect(jsonPath("$.acceptanceRate").value(37.5))
                .andExpect(jsonPath("$.averageDispatchTimeSeconds").value(48.25))
                .andExpect(jsonPath("$.driverUtilizationRate").value(66.67));
    }
}
