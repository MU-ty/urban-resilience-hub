package com.example.resilience.domain;

import jakarta.persistence.*;
import java.time.Instant;
import static com.example.resilience.domain.Enums.*;

@Entity
@Table(name = "relief_request")
public class ReliefRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "request_no", nullable = false, unique = true) private String requestNo;
    @Column(name = "requester_id", nullable = false) private Long requesterId;
    @Column(nullable = false) private String district;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SupplyCategory category;
    @Column(nullable = false) private int quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Priority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RequestStatus status;
    @Version private long version;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private Instant updatedAt;

    protected ReliefRequest() {}
    public ReliefRequest(String requestNo, Long requesterId, String district, SupplyCategory category,
                         int quantity, Priority priority) {
        this.requestNo = requestNo;
        this.requesterId = requesterId;
        this.district = district;
        this.category = category;
        this.quantity = quantity;
        this.priority = priority;
        this.status = RequestStatus.PENDING;
    }
    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public String getRequestNo() { return requestNo; }
    public Long getRequesterId() { return requesterId; }
    public String getDistrict() { return district; }
    public SupplyCategory getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public Priority getPriority() { return priority; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void allocated(boolean complete) {
        if (status != RequestStatus.PENDING && status != RequestStatus.PARTIALLY_ALLOCATED) {
            throw new IllegalStateException("当前状态不可分配");
        }
        status = complete ? RequestStatus.ALLOCATED : RequestStatus.PARTIALLY_ALLOCATED;
    }
}
