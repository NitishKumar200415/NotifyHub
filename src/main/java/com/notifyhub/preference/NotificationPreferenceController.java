package com.notifyhub.preference;

import com.notifyhub.auth.AppUserDetails;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.preference.dto.NotificationPreferenceResponse;
import com.notifyhub.preference.dto.UpdateNotificationPreferenceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences(
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {

        return ResponseEntity.ok(
                preferenceService.getPreferences(
                        userDetails.getAppUser()
                )
        );
    }

    @PutMapping("/{channel}")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable NotificationChannel channel,
            @RequestBody UpdateNotificationPreferenceRequest request
    ) {

        return ResponseEntity.ok(
                preferenceService.updatePreference(
                        userDetails.getAppUser(),
                        channel,
                        request.isEnabled()
                )
        );
    }
}