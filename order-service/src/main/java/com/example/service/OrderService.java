package com.example.service;

import com.example.model.EventType;
import com.example.model.Order;
import com.example.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.new-orders}")
    private String newOrdersTopic;

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Async
    public CompletableFuture<OrderEvent> createOrder(Order order) {
        log.info("Creating order: {}", order);

        OrderEvent event = new OrderEvent(
                order.getId(),
                EventType.ORDER_CREATED,
                order
        );

        // Используем customerId в качестве ключа для гарантии порядка обработки
        String key = order.getCustomerId();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(newOrdersTopic, key, event);

        return future.handle((result, ex) -> {
            if (ex != null) {
                log.error("Unable to send message to Kafka: {}", ex.getMessage());
                throw new RuntimeException("Failed to send order event", ex);
            }

            log.info("Order event sent successfully: {}", event);
            return event;
        });
    }

    public void updateOrderStatus(String orderId, String status) {
        // Реализация обновления статуса заказа в БД
        log.info("Updating order {} status to {}", orderId, status);
    }
}
