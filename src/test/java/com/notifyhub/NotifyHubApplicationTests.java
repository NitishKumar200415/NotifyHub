package com.notifyhub;

import org.junit.jupiter.api.Test;


/**
 * Sanity test: fails fast if the Spring context can't wire up
 * (wrong datasource config, missing bean, etc).
 * Uses Testcontainers for PostgreSQL, Redis, and RabbitMQ.
 */
class NotifyHubApplicationTests extends IntegrationTest {

    @Test
    void contextLoads() {
    }

}
