package com.example.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;
@Data
public class OrderEvent {
    private String eventId;
    private String orderId;
    private EventType eventType;
    private Order payload;
    private Instant timestamp;

    public OrderEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public OrderEvent(String orderId, EventType eventType, Order payload) {
        this();
        this.orderId = orderId;
        this.eventType = eventType;
        this.payload = payload;
    }
}
