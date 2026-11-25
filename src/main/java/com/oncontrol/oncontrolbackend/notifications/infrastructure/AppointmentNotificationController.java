package com.oncontrol.oncontrolbackend.notifications.infrastructure;

import com.oncontrol.oncontrolbackend.notifications.application.AppointmentNotificationService;
import com.oncontrol.oncontrolbackend.notifications.application.AppointmentNotificationService.Inbox;
import com.oncontrol.oncontrolbackend.notifications.application.AppointmentNotificationService.NotificationView;
import com.oncontrol.oncontrolbackend.profiles.domain.model.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/me")
public class AppointmentNotificationController {
    private final AppointmentNotificationService service;

    @GetMapping
    public Inbox list(@AuthenticationPrincipal Profile profile, @RequestParam(required = false) Long beforeId) {
        return service.list(recipient(profile), beforeId);
    }

    @PutMapping("/{id}/read")
    public NotificationView markRead(@AuthenticationPrincipal Profile profile, @PathVariable Long id) {
        return service.markRead(recipient(profile), id);
    }

    private Long recipient(Profile profile) {
        if (profile == null || !profile.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return profile.getId();
    }
}
