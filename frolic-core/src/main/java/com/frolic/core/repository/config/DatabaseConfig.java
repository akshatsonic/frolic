package com.frolic.core.repository.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.frolic.core.repository.jpa")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // JPA Auditing and repository configuration
}
