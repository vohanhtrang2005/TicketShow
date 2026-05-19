package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    // Lấy tất cả zone của một venue
    List<Zone> findByVenueId(Long venueId);

    // Kiểm tra tên zone trùng trong cùng một venue
    boolean existsByVenueIdAndNameIgnoreCase(Long venueId, String name);
}
