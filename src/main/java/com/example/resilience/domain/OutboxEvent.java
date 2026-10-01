package com.example.resilience.domain;

import jakarta.persistence.*;
import java.time.Instant;
import static com.example.resilience.domain.Enums.OutboxStatus;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "aggregate_type", nullable = false) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false) private String aggregateId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(nullable = false, columnDefinition = "json") private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OutboxStatus status;
    @Column(name = "retry_count", nullable = false) private int retryCount;
    @Column(name = "next_retry_at", nullable = false) private Instant nextRetryAt;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;

    protected OutboxEvent() {}
    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public void published() { status = OutboxStatus.PUBLISHED; publishedAt = Instant.now(); }
    public void failed() {
        retryCount++;
        status = retryCount >= 10 ? OutboxStatus.FAILED : OutboxStatus.PENDING;
        nextRetryAt = Instant.now().plusSeconds(Math.min(300, 1L << Math.min(retryCount, 8)));
    }
}
