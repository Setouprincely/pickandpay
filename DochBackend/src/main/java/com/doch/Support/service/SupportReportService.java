package com.doch.Support.service;

import com.doch.Support.dto.SupportReportRequestDTO;
import com.doch.Support.dto.SupportReportResponseDTO;
import com.doch.User.model.User;

import java.util.List;

public interface SupportReportService {

    SupportReportResponseDTO submitReport(User reporter, SupportReportRequestDTO request);

    List<SupportReportResponseDTO> getMyReports(User reporter);

    List<SupportReportResponseDTO> getAllReports(String status);

    SupportReportResponseDTO updateStatus(String reportId, String status, String resolutionNote);
}
