package com.example.resilience.repository;

import com.example.resilience.domain.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {
    List<Shelter> findByDistrictOrderByOccupancyAsc(String district);
}
