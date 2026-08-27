package com.notifyhub.preference;

import com.notifyhub.IntegrationTest;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.preference.dto.NotificationPreferenceResponse;
import com.notifyhub.user.AppUser;
import com.notifyhub.user.AppUserRepository;
import com.notifyhub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPreferenceIntegrationTest extends IntegrationTest {

    @Autowired
    private NotificationPreferenceService preferenceService;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    // =========================================================
    // DEFAULT PREFERENCES TEST
    // =========================================================

    @Test
    void shouldReturnAllChannelsEnabledByDefault() {

        AppUser user = AppUser.builder()
                .email("preferences-default@test.com")
                .passwordHash("test-password-hash")
                .role(UserRole.CLIENT)
                .build();

        AppUser savedUser =
                appUserRepository.save(user);

        List<NotificationPreferenceResponse> preferences =
                preferenceService.getPreferences(savedUser);

        assertThat(preferences)
                .hasSize(NotificationChannel.values().length);

        assertThat(preferences)
                .allMatch(NotificationPreferenceResponse::enabled);
    }


    // =========================================================
    // DISABLE CHANNEL TEST
    // =========================================================

    @Test
    void shouldDisableNotificationChannel() {

        AppUser user = AppUser.builder()
                .email("preferences-disable@test.com")
                .passwordHash("test-password-hash")
                .role(UserRole.CLIENT)
                .build();

        AppUser savedUser =
                appUserRepository.save(user);

        NotificationPreferenceResponse response =
                preferenceService.updatePreference(
                        savedUser,
                        NotificationChannel.EMAIL,
                        false
                );

        assertThat(response.channel())
                .isEqualTo(NotificationChannel.EMAIL);

        assertThat(response.enabled())
                .isFalse();

        boolean emailEnabled =
                preferenceService.isEnabled(
                        savedUser,
                        NotificationChannel.EMAIL
                );

        boolean smsEnabled =
                preferenceService.isEnabled(
                        savedUser,
                        NotificationChannel.SMS
                );

        boolean pushEnabled =
                preferenceService.isEnabled(
                        savedUser,
                        NotificationChannel.PUSH
                );

        assertThat(emailEnabled)
                .isFalse();

        assertThat(smsEnabled)
                .isTrue();

        assertThat(pushEnabled)
                .isTrue();
    }


    // =========================================================
    // UPDATE EXISTING PREFERENCE TEST
    // =========================================================

    @Test
    void shouldUpdateExistingPreferenceInsteadOfCreatingDuplicate() {

        AppUser user = AppUser.builder()
                .email("preferences-update@test.com")
                .passwordHash("test-password-hash")
                .role(UserRole.CLIENT)
                .build();

        AppUser savedUser =
                appUserRepository.save(user);

        preferenceService.updatePreference(
                savedUser,
                NotificationChannel.EMAIL,
                false
        );

        preferenceService.updatePreference(
                savedUser,
                NotificationChannel.EMAIL,
                true
        );

        List<NotificationPreference> preferences =
                preferenceRepository.findAll();

        List<NotificationPreference> emailPreferences =
                preferences.stream()
                        .filter(preference ->
                                preference.getUser()
                                        .getId()
                                        .equals(savedUser.getId())
                        )
                        .filter(preference ->
                                preference.getChannel()
                                        == NotificationChannel.EMAIL
                        )
                        .toList();

        assertThat(emailPreferences)
                .hasSize(1);

        assertThat(emailPreferences.get(0).isEnabled())
                .isTrue();
    }
}