package com.example.englishlearningplatform.repository;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresIntegrationTestBase {
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=300");

    static {
        postgres.start();
    }
}