package com.example.controller;

import com.example.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/status/{orderId}")
    public ResponseEntity<String> getShippingStatus(@PathVariable String orderId) {
        String status = shippingService.getShippingStatus(orderId);
        return ResponseEntity.ok(status);
    }
}
