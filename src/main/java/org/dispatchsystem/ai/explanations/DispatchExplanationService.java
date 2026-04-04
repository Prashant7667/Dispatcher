package org.dispatchsystem.ai.explanations;

import org.dispatchsystem.ai.client.AiNarrativeClient;
import org.dispatchsystem.ai.dto.AiDriverDecisionExplanationDTO;
import org.dispatchsystem.ai.dto.AiRideExplanationRequest;
import org.dispatchsystem.ai.dto.AiRideExplanationResponse;
import org.dispatchsystem.ai.prompts.RideNarrativePromptBuilder;
import org.dispatchsystem.common.events.domains.EventType;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class DispatchExplanationService {
    private final ReasonCodeNarrativeMapper reasonCodeNarrativeMapper;
    private final RideExplanationAssembler rideExplanationAssembler;
    private final AiNarrativeClient aiNarrativeClient;
    private final RideNarrativePromptBuilder rideNarrativePromptBuilder;

    public DispatchExplanationService(ReasonCodeNarrativeMapper reasonCodeNarrativeMapper,
                                      RideExplanationAssembler rideExplanationAssembler,
                                      AiNarrativeClient aiNarrativeClient,
                                      RideNarrativePromptBuilder rideNarrativePromptBuilder) {
        this.reasonCodeNarrativeMapper = reasonCodeNarrativeMapper;
        this.rideExplanationAssembler = rideExplanationAssembler;
        this.aiNarrativeClient = aiNarrativeClient;
        this.rideNarrativePromptBuilder = rideNarrativePromptBuilder;
    }

    public AiRideExplanationResponse explainRide(Long rideId) {
        return explain(rideExplanationAssembler.buildRideExplanationRequest(rideId));
    }

    public AiRideExplanationResponse explain(AiRideExplanationRequest request) {
        List<AiRideExplanationRequest.CandidateFacts> candidates = request.getCandidates() == null
                ? List.of()
                : request.getCandidates();

        List<AiDriverDecisionExplanationDTO> selectedDrivers = candidates.stream()
                .filter(candidate -> candidate.getEventType() == EventType.DRIVER_ASSIGNED
                        || candidate.getEventType() == EventType.OFFER_ACCEPTED)
                .map(this::toDriverExplanation)
                .toList();

        List<AiDriverDecisionExplanationDTO> skippedDrivers = candidates.stream()
                .filter(candidate -> candidate.getEventType() == EventType.DRIVER_SKIPPED
                        || candidate.getEventType() == EventType.OFFER_REJECTED
                        || candidate.getEventType() == EventType.OFFER_TIMED_OUT)
                .sorted(Comparator.comparing(AiRideExplanationRequest.CandidateFacts::getDispatchAttempt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDriverExplanation)
                .toList();

        AiRideExplanationResponse response = AiRideExplanationResponse.builder()
                .rideId(request.getRideId())
                .finalStatus(request.getFinalStatus())
                .selectionExplanation(buildSelectionExplanation(selectedDrivers, skippedDrivers))
                .failureExplanation(buildFailureExplanation(request))
                .cancellationExplanation(buildCancellationExplanation(request))
                .selectedDrivers(selectedDrivers)
                .skippedDrivers(skippedDrivers)
                .build();
        response.setSelectionExplanation(aiNarrativeClient.generateRideNarrative(
                rideNarrativePromptBuilder.build(response),
                response.getSelectionExplanation()
        ));
        return response;
    }

    private String buildSelectionExplanation(List<AiDriverDecisionExplanationDTO> selectedDrivers,
                                             List<AiDriverDecisionExplanationDTO> skippedDrivers) {
        if (selectedDrivers.isEmpty()) {
            return "No driver was selected for this ride.";
        }
        AiDriverDecisionExplanationDTO selectedDriver = selectedDrivers.get(selectedDrivers.size() - 1);
        if (skippedDrivers.isEmpty()) {
            return selectedDriver.getExplanation();
        }
        return selectedDriver.getExplanation() + " Earlier candidates were skipped, rejected, or timed out.";
    }

    private String buildFailureExplanation(AiRideExplanationRequest request) {
        if (request.getOutcome() == null || request.getOutcome().getFinalEventType() != EventType.DISPATCH_FAILED) {
            return null;
        }
        return "Ride dispatch failed because " + reasonCodeNarrativeMapper.summarizeNegativeReasons(
                request.getOutcome().getFinalReasonCodes(),
                request.getOutcome().getFinalReasonDetails()
        ) + ".";
    }

    private String buildCancellationExplanation(AiRideExplanationRequest request) {
        if (request.getOutcome() == null || request.getOutcome().getFinalEventType() != EventType.RIDE_CANCELLED) {
            return null;
        }
        return "Ride was cancelled because " + reasonCodeNarrativeMapper.summarizeNegativeReasons(
                request.getOutcome().getFinalReasonCodes(),
                request.getOutcome().getFinalReasonDetails()
        ) + ".";
    }

    private AiDriverDecisionExplanationDTO toDriverExplanation(AiRideExplanationRequest.CandidateFacts candidate) {
        return AiDriverDecisionExplanationDTO.builder()
                .driverId(candidate.getDriverId())
                .driverName(candidate.getDriverName())
                .dispatchAttempt(candidate.getDispatchAttempt())
                .dispatchRank(candidate.getDispatchRank())
                .eventType(candidate.getEventType())
                .pickupDistanceKm(candidate.getPickupDistanceKm())
                .candidateScore(candidate.getScore())
                .scoreBreakdown(candidate.getScoreBreakdown())
                .explanation(buildCandidateExplanation(candidate))
                .positiveReasons(candidate.getAcceptedReasons())
                .negativeReasons(candidate.getRejectedReasons())
                .build();
    }

    private String buildCandidateExplanation(AiRideExplanationRequest.CandidateFacts candidate) {
        if (candidate.getEventType() == EventType.DRIVER_ASSIGNED) {
            return buildSelectedDriverExplanation(candidate, "was assigned after accepting the offer");
        }
        if (candidate.getEventType() == EventType.OFFER_ACCEPTED) {
            return buildSelectedDriverExplanation(candidate, "accepted the offer");
        }
        if (candidate.getEventType() == EventType.OFFER_REJECTED) {
            return candidate.getDriverName() + " was skipped because "
                    + reasonCodeNarrativeMapper.summarizeNegativeReasons(candidate.getRejectedReasons(), "the driver rejected the offer") + ".";
        }
        if (candidate.getEventType() == EventType.OFFER_TIMED_OUT) {
            return candidate.getDriverName() + " was skipped because "
                    + reasonCodeNarrativeMapper.summarizeNegativeReasons(candidate.getRejectedReasons(), "the offer timed out") + ".";
        }
        if (candidate.getEventType() == EventType.DRIVER_SKIPPED) {
            return candidate.getDriverName() + " was skipped because "
                    + reasonCodeNarrativeMapper.summarizeNegativeReasons(candidate.getRejectedReasons(), "the driver was not eligible") + ".";
        }
        if (candidate.getEventType() == EventType.OFFER_SENT) {
            return candidate.getDriverName() + " received an offer after meeting the ride constraints.";
        }
        if (candidate.getEventType() == EventType.DRIVER_EVALUATED) {
            return candidate.getDriverName() + " remained eligible because "
                    + reasonCodeNarrativeMapper.summarizePositiveReasons(candidate.getAcceptedReasons(), "the recorded ride requirements were satisfied") + ".";
        }
        return candidate.getDriverName() + " was evaluated during dispatch.";
    }

    private String buildSelectedDriverExplanation(AiRideExplanationRequest.CandidateFacts candidate, String outcomePhrase) {
        StringBuilder explanation = new StringBuilder(candidate.getDriverName())
                .append(' ')
                .append(outcomePhrase);

        if (candidate.getDispatchAttempt() != null) {
            explanation.append(" on attempt ").append(candidate.getDispatchAttempt());
        }

        List<String> evidence = new java.util.ArrayList<>();
        if (candidate.getDispatchRank() != null) {
            evidence.add("ranked #" + candidate.getDispatchRank() + " among eligible drivers");
        }
        if (candidate.getPickupDistanceKm() != null) {
            evidence.add("pickup distance was " + round(candidate.getPickupDistanceKm()) + " km");
        }
        if (candidate.getScore() != null) {
            evidence.add("candidate score was " + round(candidate.getScore()));
        }
        if (candidate.getScoreBreakdown() != null) {
            if (candidate.getScoreBreakdown().getConstraintsScore() != null) {
                evidence.add("constraint fit contributed " + round(candidate.getScoreBreakdown().getConstraintsScore()));
            }
            if (candidate.getScoreBreakdown().getDistanceScore() != null) {
                evidence.add("distance contributed " + round(candidate.getScoreBreakdown().getDistanceScore()));
            }
            if (candidate.getScoreBreakdown().getRatingScore() != null) {
                evidence.add("rating contributed " + round(candidate.getScoreBreakdown().getRatingScore()));
            }
        }
        if (!evidence.isEmpty()) {
            explanation.append(" because ").append(String.join(", ", evidence));
        }

        explanation.append('.');
        return explanation.toString();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
