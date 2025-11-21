package com.frolic.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for Frolic Gamification System
 * Single application with all services:
 * - Play Ingestion API
 * - Reward Allocation (Kafka Consumer)
 * - WebSocket Real-time Updates
 * - Coupon Management API
 * - Admin Management API
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.frolic.core", "com.frolic.services"})
@EntityScan(basePackages = "com.frolic.core.repository.entity")
@EnableScheduling
public class FrolicApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrolicApplication.class, args);
    }
}
