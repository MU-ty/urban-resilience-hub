package com.example.resilience.repository;

import com.example.resilience.domain.OutboxEvent;
import com.example.resilience.domain.Enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.status = :status and e.nextRetryAt <= :now order by e.id")
    List<OutboxEvent> findBatch(@Param("status") OutboxStatus status, @Param("now") Instant now, Pageable pageable);
}
