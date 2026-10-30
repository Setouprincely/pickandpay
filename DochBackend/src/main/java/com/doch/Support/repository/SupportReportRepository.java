package com.doch.Support.repository;

import com.doch.Support.model.ReportStatus;
import com.doch.Support.model.SupportReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportReportRepository extends JpaRepository<SupportReport, String> {

    List<SupportReport> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    List<SupportReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<SupportReport> findAllByOrderByCreatedAtDesc();
}
