package org.dispatchsystem.dispatch;

import org.dispatchsystem.dispatch.geo.DriverEligibilityService;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DispatchDecisionService {
    private  final DriverEligibilityService driverEligibilityService;
    public DispatchDecisionService(DriverEligibilityService driverEligibilityService){
        this.driverEligibilityService=driverEligibilityService;
    }
    public DispatchDecision takeDecision(Ride ride, List<Driver> drivers){
        List<DispatchCandidate>dispatchCandidateList=driverEligibilityService.evaluateAll(ride, drivers);
        List<DispatchCandidate>acceptedCandidates=new ArrayList<>();
        List<DispatchCandidate>rejectedCandidates=new ArrayList<>();
        for(DispatchCandidate dispatchCandidate:dispatchCandidateList){
            if(dispatchCandidate.isEligible()){
                acceptedCandidates.add(dispatchCandidate);
            }
            else{
                rejectedCandidates.add(dispatchCandidate);
            }
        }
        return new DispatchDecision(ride, acceptedCandidates, rejectedCandidates);
    }
}
