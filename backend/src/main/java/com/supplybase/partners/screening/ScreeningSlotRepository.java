package com.supplybase.partners.screening;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScreeningSlotRepository extends JpaRepository<ScreeningSlot, Long> {

    List<ScreeningSlot> findAllByCategoryIdAndCityIdAndStartsAtAfterOrderByStartsAt(
            Long categoryId, Long cityId, Instant after);

    /** Atomic capacity reservation: only succeeds (returns 1) while a seat remains. */
    @Modifying(clearAutomatically = true)
    @Query("update ScreeningSlot s set s.bookedCount = s.bookedCount + 1, s.version = s.version + 1 " +
            "where s.id = :id and s.bookedCount < s.capacity")
    int tryReserveSeat(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update ScreeningSlot s set s.bookedCount = s.bookedCount - 1, s.version = s.version + 1 " +
            "where s.id = :id and s.bookedCount > 0")
    int releaseSeat(@Param("id") Long id);
}
