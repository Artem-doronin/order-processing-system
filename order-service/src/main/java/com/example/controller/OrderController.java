package com.example.controller;

import com.example.model.Order;
import com.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Object>> createOrder(@RequestBody Order order) {
        log.info("Received order creation request: {}", order);

        return orderService.createOrder(order)
                .thenApply(orderEvent -> {
                    log.info("Order created successfully: {}", orderEvent.getOrderId());
                    return ResponseEntity.ok((Object) orderEvent);
                })
                .exceptionally(ex -> {
                    log.error("Error creating order: {}", ex.getMessage(), ex);

                    if (ex.getCause() instanceof HttpMessageNotReadableException) {
                        log.warn("Invalid JSON format received");
                        return ResponseEntity.badRequest()
                                .body((Object) Map.of(
                                        "error", "Invalid JSON format",
                                        "message", ex.getCause().getMessage(),
                                        "timestamp", Instant.now()
                                ));
                    }

                    if (ex.getCause() instanceof MethodArgumentNotValidException) {
                        MethodArgumentNotValidException validationEx = (MethodArgumentNotValidException) ex.getCause();
                        List<String> errors = validationEx.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.toList());

                        log.warn("Validation errors: {}", errors);
                        return ResponseEntity.badRequest()
                                .body((Object) Map.of(
                                        "error", "Validation failed",
                                        "details", errors,
                                        "timestamp", Instant.now()
                                ));
                    }

                    log.error("Internal server error during order creation", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) Map.of(
                                    "error", "Internal server error",
                                    "message", "Please try again later",
                                    "timestamp", Instant.now()
                            ));
                });
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status) {
        log.info("Updating order {} status to {}", orderId, status);
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok().build();
    }
}