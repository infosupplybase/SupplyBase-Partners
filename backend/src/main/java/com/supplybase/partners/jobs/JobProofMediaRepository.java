package com.supplybase.partners.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobProofMediaRepository extends JpaRepository<JobProofMedia, Long> {
    List<JobProofMedia> findAllByServiceRequestId(Long serviceRequestId);
}
