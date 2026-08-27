package com.notifyhub;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class IntegrationTest {

    // =========================================================
    // MOCK EMAIL SENDER
    // =========================================================

    @MockBean
    protected JavaMailSender mailSender;


    // =========================================================
    // POSTGRESQL
    // =========================================================

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("notifyhub_test")
                    .withUsername("notifyhub")
                    .withPassword("notifyhub");


    // =========================================================
    // REDIS
    // =========================================================

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);


    // =========================================================
    // RABBITMQ
    // =========================================================

    protected static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(
                    "rabbitmq:3.13-management-alpine"
            )
                    .withUser(
                            "notifyhub",
                            "notifyhub123"
                    )
                    .withVhost("/")
                    .withPermission(
                            "/",
                            "notifyhub",
                            ".*",
                            ".*",
                            ".*"
                    );


    // =========================================================
    // START CONTAINERS ONCE FOR ENTIRE TEST SUITE
    // =========================================================

    @BeforeAll
    static void startContainers() {

        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }

        if (!REDIS.isRunning()) {
            REDIS.start();
        }

        if (!RABBITMQ.isRunning()) {
            RABBITMQ.start();
        }
    }


    // =========================================================
    // TESTCONTAINER PROPERTIES
    // =========================================================

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {

        // -----------------------------------------------------
        // PostgreSQL
        // -----------------------------------------------------

        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );


        // -----------------------------------------------------
        // Redis
        // -----------------------------------------------------

        registry.add(
                "spring.data.redis.host",
                REDIS::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> REDIS.getMappedPort(6379)
        );


        // -----------------------------------------------------
        // RabbitMQ
        // -----------------------------------------------------

        registry.add(
                "spring.rabbitmq.host",
                RABBITMQ::getHost
        );

        registry.add(
                "spring.rabbitmq.port",
                RABBITMQ::getAmqpPort
        );

        registry.add(
                "spring.rabbitmq.username",
                () -> "notifyhub"
        );

        registry.add(
                "spring.rabbitmq.password",
                () -> "notifyhub123"
        );


        // -----------------------------------------------------
        // Disable RabbitMQ Consumers During Integration Tests
        // -----------------------------------------------------

        registry.add(
                "spring.rabbitmq.listener.simple.auto-startup",
                () -> false
        );


        // -----------------------------------------------------
        // JWT
        // -----------------------------------------------------

        registry.add(
                "jwt.secret",
                () -> "test-secret-key-for-notifyhub-integration-testing-123456789"
        );

        registry.add(
                "jwt.expiration-ms",
                () -> "3600000"
        );


        // -----------------------------------------------------
        // Disable Mail Health Check During Integration Tests
        // -----------------------------------------------------

        registry.add(
                "management.health.mail.enabled",
                () -> false
        );
    }
}