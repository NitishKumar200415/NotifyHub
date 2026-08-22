package com.notifyhub.preference;

import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.preference.dto.NotificationPreferenceResponse;
import com.notifyhub.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public boolean isEnabled(
            AppUser user,
            NotificationChannel channel
    ) {

        return preferenceRepository
                .findByUserAndChannel(user, channel)
                .map(NotificationPreference::isEnabled)
                .orElse(true);
    }

    public List<NotificationPreferenceResponse> getPreferences(
            AppUser user
    ) {

        return Arrays.stream(NotificationChannel.values())
                .map(channel ->
                        new NotificationPreferenceResponse(
                                channel,
                                isEnabled(user, channel)
                        )
                )
                .toList();
    }

    @Transactional
    public NotificationPreferenceResponse updatePreference(
            AppUser user,
            NotificationChannel channel,
            boolean enabled
    ) {

        NotificationPreference preference =
                preferenceRepository
                        .findByUserAndChannel(user, channel)
                        .orElseGet(() ->
                                NotificationPreference.builder()
                                        .user(user)
                                        .channel(channel)
                                        .build()
                        );

        preference.setEnabled(enabled);

        NotificationPreference savedPreference =
                preferenceRepository.save(preference);

        return new NotificationPreferenceResponse(
                savedPreference.getChannel(),
                savedPreference.isEnabled()
        );
    }
}