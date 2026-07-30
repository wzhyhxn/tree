package com.campusplant.repository;

import com.campusplant.entity.PlantSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlantSpeciesRepository extends JpaRepository<PlantSpecies, String> {
    Optional<PlantSpecies> findByName(String name);
    List<PlantSpecies> findByNameContaining(String keyword);
}
