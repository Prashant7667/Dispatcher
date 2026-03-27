package org.dispatchsystem.dispatch;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.driver.domain.Driver;

import java.util.List;
@Data
@AllArgsConstructor
public class DispatchCandidate {
    private Driver driver;
    private double pickupDistanceKm;
    private boolean eligible;
    private List<ReasonCode> acceptedReasons;
    private List<ReasonCode> rejectedReasons;
    private double score;

}
