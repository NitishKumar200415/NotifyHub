package com.notifyhub;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class IntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("notifyhub_test")
                    .withUsername("notifyhub")
                    .withPassword("notifyhub");

    @Container
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @Container
    protected static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine")
                    .withUser("notifyhub", "notifyhub123")
                    .withVhost("/")
                    .withPermission("/", "notifyhub", ".*", ".*", ".*");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Redis
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add(
                "spring.data.redis.port",
                () -> REDIS.getMappedPort(6379)
        );

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "notifyhub");
        registry.add("spring.rabbitmq.password", () -> "notifyhub123");
    }
}