package com.example.service;


import com.example.model.EventType;
import com.example.model.Order;
import com.example.model.OrderEvent;
import com.example.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payed-orders}")
    private String payedOrdersTopic;

    @KafkaListener(
            topics = "${kafka.topics.new-orders}",
            groupId = "payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrders(@Payload List<OrderEvent> events, Acknowledgment ack) {
        try {
            for (OrderEvent event : events) {
                processOrder(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing orders batch: {}", e.getMessage());
            // Можно реализовать dead letter queue
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void processOrder(OrderEvent event) {
        log.info("Processing payment for order: {}", event.getOrderId());

        Order order = event.getPayload();

        try {
            // Имитация обработки платежа
            boolean paymentSuccess = processPayment(order);

            if (paymentSuccess) {
                order.setStatus(OrderStatus.PAYMENT_COMPLETED);
                OrderEvent paidEvent = new OrderEvent(
                        order.getId(),
                        EventType.PAYMENT_SUCCESS,
                        order
                );

                kafkaTemplate.send(payedOrdersTopic, order.getCustomerId(), paidEvent);
                log.info("Payment successful for order: {}", order.getId());
            } else {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                OrderEvent failedEvent = new OrderEvent(
                        order.getId(),
                        EventType.PAYMENT_FAILED,
                        order
                );

                // Отправка в топик для неудачных платежей
                kafkaTemplate.send("failed_payments", order.getCustomerId(), failedEvent);
                log.warn("Payment failed for order: {}", order.getId());
            }
        } catch (Exception e) {
            log.error("Error processing payment for order {}: {}",
                    order.getId(), e.getMessage());
            throw e;
        }
    }

    private boolean processPayment(Order order) {
        // Имитация логики обработки платежа
        // В реальном приложении здесь будет интеграция с платежным шлюзом
        return Math.random() > 0.1; // 90% успешных платежей
    }
}
