package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.ShowImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowImageRepository extends JpaRepository<ShowImage, Long> {
    List<ShowImage> findByShowId(Long showId);
    void deleteByShowId(Long showId);
}
