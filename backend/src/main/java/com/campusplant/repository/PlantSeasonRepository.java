package com.campusplant.repository;

import com.campusplant.entity.PlantSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlantSeasonRepository extends JpaRepository<PlantSeason, Integer> {
    List<PlantSeason> findBySeason(String season);
}
