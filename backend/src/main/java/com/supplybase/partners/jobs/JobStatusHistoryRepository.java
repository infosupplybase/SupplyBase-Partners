package com.supplybase.partners.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistory, Long> {
    List<JobStatusHistory> findAllByServiceRequestIdOrderByCreatedAtAsc(Long serviceRequestId);
}
