package com.example.resilience.service;

import com.example.resilience.common.BusinessException;
import com.example.resilience.domain.*;
import com.example.resilience.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AllocationService {
    private final ReliefRequestRepository requests;
    private final SupplyLotRepository lots;
    private final AllocationRepository allocations;
    private final OutboxRepository outbox;

    public AllocationService(ReliefRequestRepository requests, SupplyLotRepository lots,
                             AllocationRepository allocations, OutboxRepository outbox) {
        this.requests = requests; this.lots = lots; this.allocations = allocations; this.outbox = outbox;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 5)
    public Result allocate(String requestNo) {
        ReliefRequest request = requests.findForUpdate(requestNo)
                .orElseThrow(() -> BusinessException.notFound("求助单不存在"));
        if (request.getStatus() != Enums.RequestStatus.PENDING) {
            throw BusinessException.conflict("求助单已分配或不处于可分配状态");
        }

        // 按过期时间排序并加行锁：FEFO + 防止并发超卖。事务尽量短，锁内不做远程调用。
        List<SupplyLot> candidates = lots.findAvailableForUpdate(request.getCategory(), request.getDistrict(), Instant.now());
        int remaining = request.getQuantity();
        List<Allocation> result = new ArrayList<>();
        for (SupplyLot lot : candidates) {
            if (remaining == 0) break;
            int amount = Math.min(remaining, lot.available());
            lot.reserve(amount);
            result.add(allocations.save(new Allocation(request.getId(), lot.getId(), amount)));
            remaining -= amount;
        }
        if (result.isEmpty()) throw BusinessException.conflict("当前辖区无可用库存");

        int allocated = request.getQuantity() - remaining;
        request.allocated(remaining == 0);
        String payload = "{\"requestNo\":\"%s\",\"allocated\":%d,\"complete\":%s}"
                .formatted(requestNo, allocated, remaining == 0);
        outbox.save(new OutboxEvent("ReliefRequest", request.getId().toString(), "allocation.created", payload));
        return new Result(requestNo, allocated, remaining, result.stream().map(Allocation::getId).toList());
    }

    public record Result(String requestNo, int allocatedQuantity, int unallocatedQuantity, List<Long> allocationIds) {}
}
