package com.notifyhub.config;

import com.notifyhub.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Day 1 sanity-check endpoint.
 * Spring Boot Actuator already exposes /actuator/health for infra-level checks.
 * This endpoint additionally proves the app can actually talk to Postgres
 * through JPA (not just that the DB container is "up").
 */
@RestController
@RequiredArgsConstructor
public class StatusController {

    private final AppUserRepository appUserRepository;

    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        long userCount = appUserRepository.count();
        return Map.of(
                "status", "UP",
                "service", "notifyhub",
                "userCount", userCount
        );
    }
}
