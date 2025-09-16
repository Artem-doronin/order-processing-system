package com.example.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        System.out.println("HEALTH ENDPOINT CALLED - " + System.currentTimeMillis());
        return "Service is running: " + System.currentTimeMillis();
    }
}
