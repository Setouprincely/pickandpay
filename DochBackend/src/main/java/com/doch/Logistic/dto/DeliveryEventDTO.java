package com.doch.Logistic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryEventDTO {
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String message;
    /** Staff only. */
    private String internalNote;
    /** Staff only. */
    private String actorName;
    private String actorRole;
    private String hubName;
    private LocalDateTime occurredAt;
}