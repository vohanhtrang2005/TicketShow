package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.ScheduleZonePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleZonePriceRepository extends JpaRepository<ScheduleZonePrice, Long> {

    List<ScheduleZonePrice> findByScheduleId(Long scheduleId);

    Optional<ScheduleZonePrice> findByScheduleIdAndZoneId(Long scheduleId, Long zoneId);

    boolean existsByScheduleIdAndZoneId(Long scheduleId, Long zoneId);

    void deleteByScheduleId(Long scheduleId);

    @Query("""
            SELECT COALESCE(SUM(b.quantity), 0) FROM Booking b
            WHERE b.schedule.id = :scheduleId
              AND b.zone.id     = :zoneId
              AND b.status NOT IN ('CANCELLED')
            """)
    Integer sumBookedQuantityByScheduleAndZone(
            @Param("scheduleId") Long scheduleId,
            @Param("zoneId")     Long zoneId
    );
}
