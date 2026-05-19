package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.ShowType;
import com.waterpark.tickershow.enums.ShowTypeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShowTypeRepository extends JpaRepository<ShowType, Long> {

    Optional<ShowType> findByName(ShowTypeName name);
}
