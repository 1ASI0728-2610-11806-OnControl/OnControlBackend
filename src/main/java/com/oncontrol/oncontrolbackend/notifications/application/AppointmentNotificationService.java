package com.oncontrol.oncontrolbackend.notifications.application;

import com.oncontrol.oncontrolbackend.appointments.domain.model.Appointment;
import com.oncontrol.oncontrolbackend.notifications.domain.AppointmentNotification;
import com.oncontrol.oncontrolbackend.notifications.infrastructure.AppointmentNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentNotificationService {
    private static final int PAGE_SIZE = 15;
    private final AppointmentNotificationRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long recipientId, Appointment appointment, AppointmentNotification.Type type) {
        repository.save(new AppointmentNotification(recipientId, appointment.getId(), type, appointment.getAppointmentDate()));
    }

    @Transactional(readOnly = true)
    public Inbox list(Long recipientId, Long beforeId) {
        var page = PageRequest.of(0, PAGE_SIZE + 1);
        var records = beforeId == null
                ? repository.findByRecipientIdOrderByIdDesc(recipientId, page)
                : repository.findByRecipientIdAndIdLessThanOrderByIdDesc(recipientId, beforeId, page);
        Long nextBeforeId = records.size() > PAGE_SIZE ? records.get(PAGE_SIZE - 1).getId() : null;
        return new Inbox(records.stream().limit(PAGE_SIZE).map(NotificationView::from).toList(),
                repository.countByRecipientIdAndReadAtIsNull(recipientId), nextBeforeId);
    }

    @Transactional
    public NotificationView markRead(Long recipientId, Long id) {
        repository.markRead(id, recipientId, Instant.now());
        return repository.findByIdAndRecipientId(id, recipientId)
                .map(NotificationView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record Inbox(List<NotificationView> items, long unreadCount, Long nextBeforeId) {}
    public record NotificationView(Long id, Long appointmentId, AppointmentNotification.Type type,
                                   Instant appointmentDate, Instant createdAt, Instant readAt) {
        static NotificationView from(AppointmentNotification value) {
            return new NotificationView(value.getId(), value.getAppointmentId(), value.getType(),
                    value.getAppointmentDate().toInstant(ZoneOffset.UTC), value.getCreatedAt(), value.getReadAt());
        }
    }
}
