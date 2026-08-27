package com.notifyhub.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.IntegrationTest;
import com.notifyhub.auth.dto.AuthResponse;
import com.notifyhub.auth.dto.RegisterRequest;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.user.AppUser;
import com.notifyhub.user.AppUserRepository;
import com.notifyhub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationControllerIntegrationTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NotificationRepository notificationRepository;


    // =========================================================
    // REGISTER HELPER
    // =========================================================

    private String registerAndGetToken(String email) {

        RegisterRequest request =
                new RegisterRequest(
                        email,
                        "password123"
                );

        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity(
                        "/api/auth/register",
                        request,
                        AuthResponse.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().getAccessToken())
                .isNotBlank();

        return response.getBody().getAccessToken();
    }


    // =========================================================
    // AUTH HEADER HELPER
    // =========================================================

    private HttpHeaders authenticatedHeaders(String token) {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(token);

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        return headers;
    }


    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    @Test
    void shouldCreateNotification() {

        String token =
                registerAndGetToken(
                        "notification-create@test.com"
                );

        Map<String, Object> requestBody =
                Map.of(
                        "recipientAddress",
                        "receiver@test.com",
                        "channel",
                        "EMAIL",
                        "payload",
                        "Hello from NotificationController test"
                );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        requestBody,
                        authenticatedHeaders(token)
                );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        "/api/notifications",
                        request,
                        Map.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getHeaders().getLocation())
                .isNotNull();

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().get("id"))
                .isNotNull();

        assertThat(response.getBody().get("recipientAddress"))
                .isEqualTo("receiver@test.com");

        assertThat(response.getBody().get("channel"))
                .isEqualTo("EMAIL");

        assertThat(response.getBody().get("status"))
                .isEqualTo("QUEUED");
    }


    // =========================================================
    // UNAUTHORIZED ACCESS
    // =========================================================

    @Test
    void shouldRejectUnauthenticatedRequest() {

        Map<String, Object> requestBody =
                Map.of(
                        "recipientAddress",
                        "receiver@test.com",
                        "channel",
                        "EMAIL",
                        "payload",
                        "Unauthorized test"
                );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "/api/notifications",
                        requestBody,
                        String.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }


    // =========================================================
    // GET NOTIFICATION BY ID
    // =========================================================

    @Test
    void shouldGetNotificationById() {

        String email =
                "notification-get@test.com";

        String token =
                registerAndGetToken(email);

        AppUser user =
                appUserRepository
                        .findByEmail(email)
                        .orElseThrow();

        Notification notification =
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress(
                                "receiver@test.com"
                        )
                        .channel(
                                NotificationChannel.EMAIL
                        )
                        .payload("Test notification")
                        .status(
                                NotificationStatus.QUEUED
                        )
                        .retryCount(0)
                        .build();

        Notification savedNotification =
                notificationRepository.save(notification);

        HttpEntity<Void> request =
                new HttpEntity<>(
                        authenticatedHeaders(token)
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        "/api/notifications/"
                                + savedNotification.getId(),
                        org.springframework.http.HttpMethod.GET,
                        request,
                        Map.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(
                ((Number) response.getBody().get("id"))
                        .longValue()
        ).isEqualTo(savedNotification.getId());

        assertThat(
                response.getBody()
                        .get("recipientAddress")
        ).isEqualTo("receiver@test.com");

        assertThat(
                response.getBody()
                        .get("channel")
        ).isEqualTo("EMAIL");
    }


    // =========================================================
    // GET NOTIFICATION HISTORY
    // =========================================================

    @Test
    void shouldGetNotificationHistory() {

        String email =
                "notification-history@test.com";

        String token =
                registerAndGetToken(email);

        AppUser user =
                appUserRepository
                        .findByEmail(email)
                        .orElseThrow();

        Notification notification1 =
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress("one@test.com")
                        .channel(NotificationChannel.EMAIL)
                        .payload("Notification one")
                        .status(NotificationStatus.QUEUED)
                        .retryCount(0)
                        .build();

        Notification notification2 =
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress("two@test.com")
                        .channel(NotificationChannel.SMS)
                        .payload("Notification two")
                        .status(NotificationStatus.SENT)
                        .retryCount(0)
                        .build();

        notificationRepository.save(notification1);

        notificationRepository.save(notification2);

        HttpEntity<Void> request =
                new HttpEntity<>(
                        authenticatedHeaders(token)
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        "/api/notifications?page=0&size=10",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        Map.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().get("content"))
                .isNotNull();
    }


    // =========================================================
    // FILTER BY STATUS
    // =========================================================

    @Test
    void shouldFilterNotificationsByStatus() {

        String email =
                "notification-status@test.com";

        String token =
                registerAndGetToken(email);

        AppUser user =
                appUserRepository
                        .findByEmail(email)
                        .orElseThrow();

        notificationRepository.save(
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress("queued@test.com")
                        .channel(NotificationChannel.EMAIL)
                        .payload("Queued")
                        .status(NotificationStatus.QUEUED)
                        .retryCount(0)
                        .build()
        );

        notificationRepository.save(
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress("sent@test.com")
                        .channel(NotificationChannel.EMAIL)
                        .payload("Sent")
                        .status(NotificationStatus.SENT)
                        .retryCount(0)
                        .build()
        );

        HttpEntity<Void> request =
                new HttpEntity<>(
                        authenticatedHeaders(token)
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        "/api/notifications"
                                + "?status=QUEUED",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        Map.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(
                response.getBody().get("content")
        ).isNotNull();
    }


    // =========================================================
    // NOTIFICATION STATISTICS
    // =========================================================

    @Test
    void shouldGetNotificationStatistics() {

        String email =
                "notification-stats@test.com";

        String token =
                registerAndGetToken(email);

        AppUser user =
                appUserRepository
                        .findByEmail(email)
                        .orElseThrow();

        notificationRepository.save(
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress("one@test.com")
                        .channel(NotificationChannel.EMAIL)
                        .payload("One")
                        .status(NotificationStatus.QUEUED)
                        .retryCount(0)
                        .build()
        );

        notificationRepository.save(
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress("two@test.com")
                        .channel(NotificationChannel.EMAIL)
                        .payload("Two")
                        .status(NotificationStatus.SENT)
                        .retryCount(0)
                        .build()
        );

        notificationRepository.save(
                Notification.builder()
                        .recipientUser(user)
                        .recipientAddress("three@test.com")
                        .channel(NotificationChannel.SMS)
                        .payload("Three")
                        .status(NotificationStatus.FAILED)
                        .retryCount(4)
                        .build()
        );

        HttpEntity<Void> request =
                new HttpEntity<>(
                        authenticatedHeaders(token)
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        "/api/notifications/stats",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        Map.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(
                ((Number) response.getBody().get("total"))
                        .longValue()
        ).isEqualTo(3);

        assertThat(
                ((Number) response.getBody().get("queued"))
                        .longValue()
        ).isEqualTo(1);

        assertThat(
                ((Number) response.getBody().get("sent"))
                        .longValue()
        ).isEqualTo(1);

        assertThat(
                ((Number) response.getBody().get("failed"))
                        .longValue()
        ).isEqualTo(1);
    }


    // =========================================================
    // USER CANNOT ACCESS ANOTHER USER'S NOTIFICATION
    // =========================================================

    @Test
    void shouldNotAllowUserToAccessAnotherUsersNotification() {

        String firstUserEmail =
                "first-user@test.com";

        String firstUserToken =
                registerAndGetToken(firstUserEmail);

        AppUser firstUser =
                appUserRepository
                        .findByEmail(firstUserEmail)
                        .orElseThrow();

        Notification notification =
                notificationRepository.save(
                        Notification.builder()
                                .recipientUser(firstUser)
                                .recipientAddress(
                                        "private@test.com"
                                )
                                .channel(
                                        NotificationChannel.EMAIL
                                )
                                .payload("Private notification")
                                .status(
                                        NotificationStatus.QUEUED
                                )
                                .retryCount(0)
                                .build()
                );

        String secondUserToken =
                registerAndGetToken(
                        "second-user@test.com"
                );

        HttpEntity<Void> request =
                new HttpEntity<>(
                        authenticatedHeaders(
                                secondUserToken
                        )
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/notifications/"
                                + notification.getId(),
                        org.springframework.http.HttpMethod.GET,
                        request,
                        String.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}