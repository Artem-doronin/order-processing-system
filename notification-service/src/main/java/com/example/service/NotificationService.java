package com.example.service;


import com.example.model.Order;
import com.example.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @KafkaListener(
            topics = "${kafka.topics.sent-orders}",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processShippedOrders(@Payload List<OrderEvent> events, Acknowledgment ack) {
        try {
            for (OrderEvent event : events) {
                sendNotification(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing shipped orders batch: {}", e.getMessage());
        }
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendNotification(OrderEvent event) {
        log.info("Sending notification for order: {}", event.getOrderId());

        Order order = event.getPayload();

        try {

            log.info("Notification sent successfully for order: {}", order.getId());
            log.info("сервис нотификации оповестил :{},{},{}", order.getCustomerEmail(),order.getStatus(),order.getTotalAmount());

        } catch (Exception e) {
            log.error("Failed to send notification for order {}: {}",
                    order.getId(), e.getMessage());
            throw e;
        }
    }

    private void sendEmailNotification(Order order) {

        log.info("Email notification sent to: {}", order.getCustomerEmail());
    }

    private void sendSmsNotification(Order order) {
        // Имитация отправки SMS
        log.info("SMS notification would be sent for order: {}", order.getId());
        // В реальном приложении здесь будет интеграция с SMS-шлюзом
    }
}