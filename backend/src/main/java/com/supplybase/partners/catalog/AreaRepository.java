package com.supplybase.partners.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findAllByCityIdAndActiveTrueOrderByName(Long cityId);
}
