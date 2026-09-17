package com.supplybase.partners.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {
    List<CommunityReport> findAllByStatus(CommunityReport.Status status);
}
