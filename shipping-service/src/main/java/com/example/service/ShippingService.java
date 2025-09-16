package com.example.service;


import com.example.model.EventType;
import com.example.model.Order;
import com.example.model.OrderEvent;
import com.example.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ShippingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.sent-orders}")
    private String sentOrdersTopic;

    @KafkaListener(
            topics = "${kafka.topics.payed-orders}",
            groupId = "shipping-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processPaidOrders(@Payload List<OrderEvent> events, Acknowledgment ack) {
        try {
            for (OrderEvent event : events) {
                processOrderShipping(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing paid orders batch: {}", e.getMessage());
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void processOrderShipping(OrderEvent event) {
        log.info("Processing shipping for order: {}", event.getOrderId());

        Order order = event.getPayload();

        try {
            // Имитация процесса упаковки и отправки
            boolean shippingSuccess = processShipping(order);

            if (shippingSuccess) {
                order.setStatus(OrderStatus.SHIPPED);
                OrderEvent shippedEvent = new OrderEvent(
                        order.getId(),
                        EventType.SHIPMENT_SUCCESS,
                        order
                );

                kafkaTemplate.send(sentOrdersTopic, order.getCustomerId(), shippedEvent);
                log.info("Order {} shipped successfully", order.getId());
            } else {
                order.setStatus(OrderStatus.CANCELLED);
                OrderEvent failedEvent = new OrderEvent(
                        order.getId(),
                        EventType.SHIPMENT_FAILED,
                        order
                );

                kafkaTemplate.send("failed_shipments", order.getCustomerId(), failedEvent);
                log.warn("Shipping failed for order: {}", order.getId());
            }
        } catch (Exception e) {
            log.error("Error processing shipping for order {}: {}",
                    order.getId(), e.getMessage());
            throw e;
        }
    }

    private boolean processShipping(Order order) {
        // Имитация логики обработки отгрузки
        // В реальном приложении здесь будет интеграция с логистическими службами
        log.info("Packaging order {} for customer {}", order.getId(), order.getCustomerId());

        // Симуляция процесса упаковки
        try {
            Thread.sleep(500); // Имитация времени на упаковку
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 95% успешных отгрузок
        return Math.random() > 0.05;
    }

    // REST endpoint для проверки статуса отгрузки
    public String getShippingStatus(String orderId) {
        // В реальном приложении здесь будет запрос к базе данных
        return "SHIPPED";
    }
}
