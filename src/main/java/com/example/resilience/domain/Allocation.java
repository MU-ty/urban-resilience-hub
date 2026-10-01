package com.example.resilience.domain;

import jakarta.persistence.*;
import java.time.Instant;
import static com.example.resilience.domain.Enums.AllocationStatus;

@Entity
public class Allocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "request_id", nullable = false) private Long requestId;
    @Column(name = "supply_lot_id", nullable = false) private Long supplyLotId;
    @Column(nullable = false) private int quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AllocationStatus status;
    @Column(name = "allocated_at", nullable = false, insertable = false, updatable = false) private Instant allocatedAt;

    protected Allocation() {}
    public Allocation(Long requestId, Long supplyLotId, int quantity) {
        this.requestId = requestId;
        this.supplyLotId = supplyLotId;
        this.quantity = quantity;
        this.status = AllocationStatus.RESERVED;
    }
    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public Long getSupplyLotId() { return supplyLotId; }
    public int getQuantity() { return quantity; }
    public AllocationStatus getStatus() { return status; }
}
