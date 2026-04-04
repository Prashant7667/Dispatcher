package org.dispatchsystem.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiOpsSummaryRequest {
    private LocalDateTime from;
    private LocalDateTime to;
}
