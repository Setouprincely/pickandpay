package com.doch.Support.service.impl;

import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import com.doch.Support.dto.SupportReportRequestDTO;
import com.doch.Support.dto.SupportReportResponseDTO;
import com.doch.Support.model.ReportStatus;
import com.doch.Support.model.SupportReport;
import com.doch.Support.repository.SupportReportRepository;
import com.doch.Support.service.SupportReportService;
import com.doch.User.model.User;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportReportServiceImpl implements SupportReportService {

    private final SupportReportRepository supportReportRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public SupportReportResponseDTO submitReport(User reporter, SupportReportRequestDTO request) {
        SupportReport report = SupportReport.builder()
                .reporter(reporter)
                .category(request.getCategory().trim().toUpperCase())
                .subject(request.getSubject())
                .description(request.getDescription())
                .deliveryId(request.getDeliveryId())
                .status(ReportStatus.OPEN)
                .build();

        supportReportRepository.save(report);
        log.info("SUPPORT: New problem report {} from {} [{}]", report.getId(), reporter.getEmail(), report.getCategory());

        // Alert admins in-app so reports appear in the dashboard notification feed
        String alert = String.format("New problem report from %s %s: [%s] %s",
                reporter.getFirstName(), reporter.getLastName(), report.getCategory(), report.getSubject());
        notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT, alert);
        notificationService.sendNotificationToRole("ROLE_XENDER_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT, alert);

        return SupportReportResponseDTO.fromReport(report);
    }

    @Override
    public List<SupportReportResponseDTO> getMyReports(User reporter) {
        return supportReportRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId())
                .stream().map(SupportReportResponseDTO::fromReport).toList();
    }

    @Override
    public List<SupportReportResponseDTO> getAllReports(String status) {
        List<SupportReport> reports;
        if (StringUtils.hasText(status)) {
            reports = supportReportRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status));
        } else {
            reports = supportReportRepository.findAllByOrderByCreatedAtDesc();
        }
        return reports.stream().map(SupportReportResponseDTO::fromReport).toList();
    }

    @Override
    @Transactional
    public SupportReportResponseDTO updateStatus(String reportId, String status, String resolutionNote) {
        SupportReport report = supportReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Support report not found: " + reportId));

        report.setStatus(parseStatus(status));
        if (StringUtils.hasText(resolutionNote)) {
            report.setResolutionNote(resolutionNote);
        }
        supportReportRepository.save(report);

        // Tell the reporter their issue was handled
        if (report.getStatus() == ReportStatus.RESOLVED) {
            notificationService.sendNotification(report.getReporter(), NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Your problem report \"" + report.getSubject() + "\" has been resolved."
                            + (StringUtils.hasText(resolutionNote) ? " Note: " + resolutionNote : ""));
        }

        return SupportReportResponseDTO.fromReport(report);
    }

    private ReportStatus parseStatus(String raw) {
        try {
            return ReportStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new BusinessException("Invalid report status: " + raw
                    + ". Allowed values: OPEN, IN_REVIEW, RESOLVED, DISMISSED");
        }
    }
}
