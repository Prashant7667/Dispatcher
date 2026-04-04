package org.dispatchsystem.ride.controller;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.common.exceptions.GlobalExceptionHandler;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.dto.DispatchAnalyticsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchOpsSummaryDTO;
import org.dispatchsystem.ride.dto.DispatchRideExplanationDTO;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.dispatchsystem.ride.service.DispatchInsightService;
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
    private DispatchInsightService dispatchInsightService;

    @BeforeEach
    void setUp() {
        dispatchAuditService = mock(DispatchAuditService.class);
        dispatchInsightService = mock(DispatchInsightService.class);
        DispatchAdminController controller = new DispatchAdminController(dispatchAuditService, dispatchInsightService);
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
        LocalDateTime from = LocalDateTime.of(2026, 3, 30, 1, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 30, 7, 0);
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

        when(dispatchAuditService.getDispatchAnalyticsSummary(from, to)).thenReturn(summary);

        mockMvc.perform(get("/admin/analytics/dispatch-summary")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRides").value(12))
                .andExpect(jsonPath("$.acceptedOffers").value(3))
                .andExpect(jsonPath("$.acceptanceRate").value(37.5))
                .andExpect(jsonPath("$.averageDispatchTimeSeconds").value(48.25))
                .andExpect(jsonPath("$.driverUtilizationRate").value(66.67));
    }

    @Test
    void getRideExplanationReturnsStructuredNarrative() throws Exception {
        DispatchRideExplanationDTO explanation = DispatchRideExplanationDTO.builder()
                .rideId(44L)
                .selectionExplanation("Nisha was selected after earlier candidates timed out.")
                .selectedDrivers(List.of(
                        DispatchRideExplanationDTO.DriverDecisionDTO.builder()
                                .driverId(9L)
                                .driverName("Nisha")
                                .dispatchAttempt(2)
                                .explanation("Nisha accepted the offer on attempt 2.")
                                .positiveReasons(List.of(ReasonCode.DRIVER_ACCEPTED))
                                .negativeReasons(List.of())
                                .build()
                ))
                .skippedDrivers(List.of())
                .build();

        when(dispatchInsightService.getRideExplanation(44L)).thenReturn(explanation);

        mockMvc.perform(get("/admin/rides/44/dispatch-explanation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(44))
                .andExpect(jsonPath("$.selectionExplanation").value("Nisha was selected after earlier candidates timed out."))
                .andExpect(jsonPath("$.selectedDrivers[0].driverName").value("Nisha"));
    }

    @Test
    void getOpsSummaryReturnsAdminInsights() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 3, 30, 1, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 30, 7, 0);
        DispatchOpsSummaryDTO summary = DispatchOpsSummaryDTO.builder()
                .from(from)
                .to(to)
                .acceptanceRate(42.86)
                .averageDispatchTimeSeconds(52.15)
                .topCancellationReasons(List.of(
                        DispatchOpsSummaryDTO.ReasonCountDTO.builder()
                                .reasonCode(ReasonCode.PASSENGER_CANCELLED)
                                .count(5)
                                .build()
                ))
                .topDispatchFailureReasons(List.of(
                        DispatchOpsSummaryDTO.ReasonCountDTO.builder()
                                .reasonCode(ReasonCode.NO_ELIGIBLE_DRIVERS)
                                .count(4)
                                .build()
                ))
                .lowSupplyZones(List.of(
                        DispatchOpsSummaryDTO.ZonePressureDTO.builder()
                                .zoneLabel("28.61, 77.21")
                                .noSupplyCount(3)
                                .build()
                ))
                .narrative("Acceptance rate was 42.86%.")
                .build();

        when(dispatchInsightService.getOpsSummary(from, to)).thenReturn(summary);

        mockMvc.perform(get("/admin/analytics/ops-summary")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptanceRate").value(42.86))
                .andExpect(jsonPath("$.topCancellationReasons[0].reasonCode").value("PASSENGER_CANCELLED"))
                .andExpect(jsonPath("$.lowSupplyZones[0].zoneLabel").value("28.61, 77.21"));
    }
}
