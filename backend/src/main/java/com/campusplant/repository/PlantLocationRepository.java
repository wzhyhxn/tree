package com.campusplant.repository;

import com.campusplant.entity.PlantLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlantLocationRepository extends JpaRepository<PlantLocation, Integer> {
    Optional<PlantLocation> findBySpeciesId(String speciesId);
    void deleteBySpeciesId(String speciesId);
}
