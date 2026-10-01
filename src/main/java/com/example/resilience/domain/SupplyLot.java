package com.example.resilience.domain;

import jakarta.persistence.*;
import java.time.Instant;
import static com.example.resilience.domain.Enums.SupplyCategory;

@Entity
@Table(name = "supply_lot")
public class SupplyLot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "shelter_id", nullable = false) private Long shelterId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SupplyCategory category;
    @Column(nullable = false) private int quantity;
    @Column(name = "reserved_quantity", nullable = false) private int reservedQuantity;
    @Column(name = "expires_at") private Instant expiresAt;
    @Version private long version;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;

    protected SupplyLot() {}
    public Long getId() { return id; }
    public Long getShelterId() { return shelterId; }
    public SupplyCategory getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public Instant getExpiresAt() { return expiresAt; }
    public int available() { return quantity - reservedQuantity; }
    public void reserve(int amount) {
        if (amount <= 0 || available() < amount) throw new IllegalArgumentException("库存不足");
        reservedQuantity += amount;
    }
}
