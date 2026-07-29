package com.notifyhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sanity test: fails fast if the Spring context can't wire up
 * (wrong datasource config, missing bean, etc).
 * Requires the Postgres container from docker-compose.yml to be running.
 */
@SpringBootTest
class NotifyHubApplicationTests {

    @Test
    void contextLoads() {
    }

}
