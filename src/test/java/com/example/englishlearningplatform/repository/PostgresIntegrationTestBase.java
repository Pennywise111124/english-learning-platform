package com.example.englishlearningplatform.repository;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

abstract class PostgresIntegrationTestBase {
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }
}