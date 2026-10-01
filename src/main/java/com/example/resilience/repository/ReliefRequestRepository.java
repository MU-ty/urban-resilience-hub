package com.example.resilience.repository;

import com.example.resilience.domain.ReliefRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ReliefRequestRepository extends JpaRepository<ReliefRequest, Long>, JpaSpecificationExecutor<ReliefRequest> {
    Optional<ReliefRequest> findByRequestNo(String requestNo);
    Optional<ReliefRequest> findByRequesterIdAndIdempotencyKey(Long requesterId, String idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReliefRequest r where r.requestNo = :requestNo")
    Optional<ReliefRequest> findForUpdate(@Param("requestNo") String requestNo);
}
