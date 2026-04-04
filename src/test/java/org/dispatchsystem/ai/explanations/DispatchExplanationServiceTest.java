package org.dispatchsystem.ai.explanations;

import org.dispatchsystem.ai.client.AiNarrativeClient;
import org.dispatchsystem.ai.dto.AiRideExplanationRequest;
import org.dispatchsystem.ai.dto.AiRideExplanationResponse;
import org.dispatchsystem.ai.prompts.RideNarrativePromptBuilder;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.ride.domain.RideStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

class DispatchExplanationServiceTest {

    @Test
    void explainBuildsSelectedSkippedAndFailureNarrative() {
        AiNarrativeClient aiNarrativeClient = mock(AiNarrativeClient.class);
        when(aiNarrativeClient.generateRideNarrative(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        DispatchExplanationService service = new DispatchExplanationService(
                new ReasonCodeNarrativeMapper(),
                mock(RideExplanationAssembler.class),
                aiNarrativeClient,
                new RideNarrativePromptBuilder()
        );

        AiRideExplanationRequest request = AiRideExplanationRequest.builder()
                .rideId(55L)
                .finalStatus(RideStatus.CANCELLED)
                .candidates(List.of(
                        AiRideExplanationRequest.CandidateFacts.builder()
                                .driverId(9L)
                                .driverName("Nisha")
                                .dispatchAttempt(1)
                                .eventType(EventType.OFFER_REJECTED)
                                .acceptedReasons(List.of(ReasonCode.BOOKING_TYPE_SUPPORTED))
                                .rejectedReasons(List.of(ReasonCode.DRIVER_REJECTED))
                                .build(),
                        AiRideExplanationRequest.CandidateFacts.builder()
                                .driverId(10L)
                                .driverName("Ravi")
                                .dispatchAttempt(2)
                                .dispatchRank(1)
                                .eventType(EventType.DRIVER_ASSIGNED)
                                .pickupDistanceKm(1.8)
                                .score(6.0)
                                .scoreBreakdown(AiRideExplanationRequest.ScoreBreakdown.builder()
                                        .constraintsScore(3.0)
                                        .distanceScore(1.8)
                                        .ratingScore(1.2)
                                        .build())
                                .acceptedReasons(List.of(ReasonCode.DRIVER_ACCEPTED, ReasonCode.SCHEDULE_AVAILABLE))
                                .rejectedReasons(List.of())
                                .build()
                ))
                .outcome(AiRideExplanationRequest.OutcomeFacts.builder()
                        .finalEventType(EventType.RIDE_CANCELLED)
                        .assignedDriverId(10L)
                        .assignedDriverName("Ravi")
                        .finalReasonCodes(List.of(ReasonCode.PASSENGER_CANCELLED))
                        .finalReasonDetails("Ride was cancelled while an offer was pending")
                        .build())
                .build();

        AiRideExplanationResponse response = service.explain(request);

        assertEquals(55L, response.getRideId());
        assertEquals(1, response.getSelectedDrivers().size());
        assertEquals(1, response.getSkippedDrivers().size());
        assertTrue(response.getSelectionExplanation().contains("Ravi"));
        assertTrue(response.getSelectionExplanation().contains("ranked #1"));
        assertTrue(response.getSelectionExplanation().contains("1.8 km"));
        assertTrue(response.getSelectionExplanation().contains("constraint fit contributed 3.0"));
        assertTrue(response.getSelectionExplanation().contains("rating contributed 1.2"));
        assertTrue(response.getSkippedDrivers().get(0).getExplanation().contains("driver rejected the offer"));
        assertTrue(response.getCancellationExplanation().contains("passenger cancelled the ride"));
    }
}
