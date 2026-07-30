package com.campusplant.repository;

import com.campusplant.entity.PlantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlantImageRepository extends JpaRepository<PlantImage, Integer> {
    List<PlantImage> findByCategory(String category);
}
