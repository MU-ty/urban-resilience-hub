package com.example.resilience.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Shelter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String district;
    @Column(nullable = false) private BigDecimal latitude;
    @Column(nullable = false) private BigDecimal longitude;
    @Column(nullable = false) private int capacity;
    @Column(nullable = false) private int occupancy;
    @Version private long version;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;

    protected Shelter() {}
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public int getCapacity() { return capacity; }
    public int getOccupancy() { return occupancy; }
}
