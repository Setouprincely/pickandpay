package com.doch.Logistic.dto.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Response containing driver activity and task completion statistics.")
public class DriverPerformanceDTO {
    @Schema(description = "Total tasks completed today", example = "5")
    private Integer completedToday;
    @Schema(description = "Total tasks completed this week", example = "28")
    private Integer completedThisWeek;
    @Schema(description = "Total tasks completed this month", example = "120")
    private Integer completedThisMonth;
    @Schema(description = "Currently active jobs")
    private Integer activeJobs;
    @Schema(description = "Average customer delivery rating")
    private Double rating;
    @Schema(description = "A short list of recently finalized deliveries")
    private List<RecentTask> recentTaskHistory;

    @Data
    @Builder
    public static class RecentTask {
        private String deliveryId;
        private String trackingNumber;
        private String completedAt;
        private String destination;
    }
}