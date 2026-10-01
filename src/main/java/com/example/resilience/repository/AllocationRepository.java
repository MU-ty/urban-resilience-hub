package com.example.resilience.repository;

import com.example.resilience.domain.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByRequestId(Long requestId);
}
