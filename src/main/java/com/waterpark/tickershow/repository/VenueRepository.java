package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    // Chỉ lấy venue đang hoạt động (khi Operator chọn venue cho schedule)
    List<Venue> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
