package com.frolic.services.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enable async processing for WebSocket result polling
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Virtual threads are already configured in VirtualThreadConfig
}
