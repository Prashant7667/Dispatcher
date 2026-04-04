package org.dispatchsystem.dispatch;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;

import java.util.Comparator;
import java.util.List;
@Data
@AllArgsConstructor
public class DispatchDecision {
    private Ride ride;
    private List<DispatchCandidate> acceptedCandidates;
    private List<DispatchCandidate> rejectedCandidates;
    public Boolean hasEligibleCandidates(){
        return !acceptedCandidates.isEmpty();
    }
    public List<Driver> rankedDrivers() {
        return acceptedCandidates.stream()
                .sorted(Comparator.comparingDouble(DispatchCandidate::getPickupDistanceKm))
                .map(DispatchCandidate::getDriver)
                .toList();
    }

}
