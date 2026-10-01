package com.example.resilience.domain;

public final class Enums {
    private Enums() {}
    public enum SupplyCategory { WATER, FOOD, MEDICINE, POWER, HYGIENE }
    public enum Priority { LOW, NORMAL, HIGH, CRITICAL }
    public enum RequestStatus { PENDING, ALLOCATED, PARTIALLY_ALLOCATED, FULFILLED, CANCELLED }
    public enum AllocationStatus { RESERVED, DISPATCHED, DELIVERED, CANCELLED }
    public enum OutboxStatus { PENDING, PUBLISHED, FAILED }
}
