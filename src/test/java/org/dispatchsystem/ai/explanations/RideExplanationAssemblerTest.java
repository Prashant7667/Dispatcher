package org.dispatchsystem.ai.explanations;

import org.dispatchsystem.ai.dto.AiRideExplanationRequest;
import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideDispatchEventRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RideExplanationAssemblerTest {

    @Test
    void buildRideExplanationRequestGroupsEventsPerDriverAndBuildsOutcome() {
        RideRepository rideRepository = mock(RideRepository.class);
        RideDispatchEventRepository rideDispatchEventRepository = mock(RideDispatchEventRepository.class);
        RideExplanationAssembler assembler = new RideExplanationAssembler(rideRepository, rideDispatchEventRepository);

        Ride ride = new Ride();
        ride.setId(44L);
        ride.setStatus(RideStatus.DRIVER_ASSIGNED);
        ride.setBookingType(BookingType.TRIP);
        ride.setRequiredLuggageCapacity(20);
        ride.setEstimatedDurationMinutes(30);
        ride.setStartLatitude(28.61);
        ride.setStartLongitude(77.21);
        ride.setDispatchStartedAt(LocalDateTime.of(2026, 4, 4, 10, 0));
        ride.setDriverAssignedAt(LocalDateTime.of(2026, 4, 4, 10, 2));

        Driver nisha = new Driver();
        nisha.setId(9L);
        nisha.setName("Nisha");

        Driver ravi = new Driver();
        ravi.setId(10L);
        ravi.setName("Ravi");

        ride.setDriver(ravi);

        RideDispatchEvent nishaEvaluated = RideDispatchEvent.builder()
                .ride(ride)
                .driver(nisha)
                .eventType(EventType.DRIVER_EVALUATED)
                .dispatchAttempt(1)
                .dispatchRank(1)
                .pickupDistanceKm(1.75)
                .candidateScore(4.0)
                .constraintsScore(2.0)
                .distanceScore(2.25)
                .ratingScore(0.0)
                .createdAt(LocalDateTime.of(2026, 4, 4, 10, 0, 5))
                .positiveReasons(List.of(ReasonCode.BOOKING_TYPE_SUPPORTED))
                .negativeReasons(List.of())
                .build();

        RideDispatchEvent nishaTimedOut = RideDispatchEvent.builder()
                .ride(ride)
                .driver(nisha)
                .eventType(EventType.OFFER_TIMED_OUT)
                .dispatchAttempt(1)
                .createdAt(LocalDateTime.of(2026, 4, 4, 10, 0, 35))
                .positiveReasons(List.of())
                .negativeReasons(List.of(ReasonCode.OFFER_TIMEOUT))
                .build();

        RideDispatchEvent raviAccepted = RideDispatchEvent.builder()
                .ride(ride)
                .driver(ravi)
                .eventType(EventType.OFFER_ACCEPTED)
                .dispatchAttempt(2)
                .dispatchRank(2)
                .pickupDistanceKm(2.25)
                .candidateScore(3.0)
                .constraintsScore(1.5)
                .distanceScore(1.5)
                .ratingScore(0.0)
                .createdAt(LocalDateTime.of(2026, 4, 4, 10, 1, 10))
                .positiveReasons(List.of(ReasonCode.DRIVER_ACCEPTED))
                .negativeReasons(List.of())
                .build();

        RideDispatchEvent raviAssigned = RideDispatchEvent.builder()
                .ride(ride)
                .driver(ravi)
                .eventType(EventType.DRIVER_ASSIGNED)
                .dispatchAttempt(2)
                .createdAt(LocalDateTime.of(2026, 4, 4, 10, 1, 12))
                .positiveReasons(List.of(ReasonCode.DRIVER_ACCEPTED))
                .negativeReasons(List.of())
                .reasonDetails("Driver assigned to ride")
                .build();

        when(rideRepository.findById(44L)).thenReturn(Optional.of(ride));
        when(rideDispatchEventRepository.findByRide_IdOrderByCreatedAtAsc(44L))
                .thenReturn(List.of(nishaEvaluated, nishaTimedOut, raviAccepted, raviAssigned));

        AiRideExplanationRequest request = assembler.buildRideExplanationRequest(44L);

        assertEquals(44L, request.getRideId());
        assertEquals(RideStatus.DRIVER_ASSIGNED, request.getFinalStatus());
        assertEquals(2, request.getCandidates().size());

        AiRideExplanationRequest.CandidateFacts firstCandidate = request.getCandidates().get(0);
        assertEquals(9L, firstCandidate.getDriverId());
        assertEquals(EventType.OFFER_TIMED_OUT, firstCandidate.getEventType());
        assertEquals(1, firstCandidate.getDispatchRank());
        assertEquals(1.75, firstCandidate.getPickupDistanceKm());
        assertEquals(4.0, firstCandidate.getScore());
        assertEquals(2.0, firstCandidate.getScoreBreakdown().getConstraintsScore());
        assertEquals(2.25, firstCandidate.getScoreBreakdown().getDistanceScore());
        assertTrue(firstCandidate.getRejectedReasons().contains(ReasonCode.OFFER_TIMEOUT));

        AiRideExplanationRequest.CandidateFacts secondCandidate = request.getCandidates().get(1);
        assertEquals(10L, secondCandidate.getDriverId());
        assertEquals(EventType.DRIVER_ASSIGNED, secondCandidate.getEventType());
        assertEquals(2, secondCandidate.getDispatchRank());
        assertEquals(2.25, secondCandidate.getPickupDistanceKm());
        assertEquals(3.0, secondCandidate.getScore());
        assertEquals(1.5, secondCandidate.getScoreBreakdown().getConstraintsScore());
        assertEquals(1.5, secondCandidate.getScoreBreakdown().getDistanceScore());
        assertTrue(secondCandidate.getAcceptedReasons().contains(ReasonCode.DRIVER_ACCEPTED));

        assertNotNull(request.getOutcome());
        assertEquals(EventType.DRIVER_ASSIGNED, request.getOutcome().getFinalEventType());
        assertEquals(10L, request.getOutcome().getAssignedDriverId());
        assertEquals("Ravi", request.getOutcome().getAssignedDriverName());
        assertEquals("Driver assigned to ride", request.getOutcome().getFinalReasonDetails());
    }
}
