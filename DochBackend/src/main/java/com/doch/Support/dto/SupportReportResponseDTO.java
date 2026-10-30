package com.doch.Support.dto;

import com.doch.Support.model.SupportReport;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SupportReportResponseDTO {

    private String id;
    private String category;
    private String subject;
    private String description;
    private String deliveryId;
    private String status;
    private String resolutionNote;

    private String reporterId;
    private String reporterName;
    private String reporterEmail;

    private Date createdAt;
    private Date updatedAt;

    public static SupportReportResponseDTO fromReport(SupportReport r) {
        return SupportReportResponseDTO.builder()
                .id(r.getId())
                .category(r.getCategory())
                .subject(r.getSubject())
                .description(r.getDescription())
                .deliveryId(r.getDeliveryId())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .resolutionNote(r.getResolutionNote())
                .reporterId(r.getReporter() != null ? r.getReporter().getId() : null)
                .reporterName(r.getReporter() != null
                        ? r.getReporter().getFirstName() + " " + r.getReporter().getLastName() : null)
                .reporterEmail(r.getReporter() != null ? r.getReporter().getEmail() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
