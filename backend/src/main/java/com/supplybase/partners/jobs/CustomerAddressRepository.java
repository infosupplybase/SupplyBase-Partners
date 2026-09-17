package com.supplybase.partners.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findAllByAreaIdIn(List<Long> areaIds);

    List<CustomerAddress> findAllByCustomerId(Long customerId);
}
