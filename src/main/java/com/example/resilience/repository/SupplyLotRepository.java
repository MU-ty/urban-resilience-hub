package com.example.resilience.repository;

import com.example.resilience.domain.SupplyLot;
import com.example.resilience.domain.Enums.SupplyCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface SupplyLotRepository extends JpaRepository<SupplyLot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from SupplyLot l join Shelter s on s.id = l.shelterId " +
           "where l.category = :category and s.district = :district " +
           "and (l.expiresAt is null or l.expiresAt > :now) and l.quantity > l.reservedQuantity " +
           "order by case when l.expiresAt is null then 1 else 0 end, l.expiresAt asc, l.id asc")
    List<SupplyLot> findAvailableForUpdate(@Param("category") SupplyCategory category,
                                           @Param("district") String district,
                                           @Param("now") Instant now);

    List<SupplyLot> findByShelterIdOrderByExpiresAtAsc(Long shelterId);
}
