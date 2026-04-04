package org.dispatchsystem.dispatch;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.driver.domain.Driver;

import java.util.List;
@Data
@AllArgsConstructor
public class DispatchCandidate {
    private Driver driver;
    private double pickupDistanceKm;
    private boolean eligible;
    private List<String> acceptedReasons;
    private List<String> rejectedReasons;
    private double score;

}
