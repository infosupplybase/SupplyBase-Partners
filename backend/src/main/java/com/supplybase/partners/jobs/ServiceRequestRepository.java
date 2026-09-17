package com.supplybase.partners.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findAllByStatusAndServiceIdInAndAddressIdInOrderByScheduledAt(
            ServiceRequest.Status status, List<Long> serviceIds, List<Long> addressIds);

    List<ServiceRequest> findAllByAssignedPartnerIdOrderByScheduledAtDesc(Long partnerId);

    /** Atomic acceptance: succeeds (returns 1) only while the job is still OPEN and unassigned. */
    @Modifying(clearAutomatically = true)
    @Query("update ServiceRequest s set s.status = com.supplybase.partners.jobs.ServiceRequest.Status.ASSIGNED, " +
            "s.assignedPartnerId = :partnerId, s.version = s.version + 1 " +
            "where s.id = :id and s.status = com.supplybase.partners.jobs.ServiceRequest.Status.OPEN and s.assignedPartnerId is null")
    int tryAssign(@Param("id") Long id, @Param("partnerId") Long partnerId);
}
