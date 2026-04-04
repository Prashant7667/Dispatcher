package org.dispatchsystem.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.common.events.domains.ReasonCode;
@Data
@AllArgsConstructor
public class DispatchFailureReasonsResponseDTO {
    ReasonCode reasonCode;
    Long count;
}
