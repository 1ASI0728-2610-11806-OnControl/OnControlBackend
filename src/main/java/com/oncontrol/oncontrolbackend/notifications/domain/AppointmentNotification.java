package com.oncontrol.oncontrolbackend.notifications.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_notifications", indexes = {
        @Index(name = "idx_appointment_notifications_recipient", columnList = "recipient_id, id"),
        @Index(name = "idx_appointment_notifications_unread", columnList = "recipient_id, read_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentNotification {
    public enum Type { SCHEDULED, CANCELLED, RESCHEDULED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false)
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private LocalDateTime appointmentDate;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    public AppointmentNotification(Long recipientId, Long appointmentId, Type type, LocalDateTime appointmentDate) {
        this.recipientId = recipientId;
        this.appointmentId = appointmentId;
        this.type = type;
        this.appointmentDate = appointmentDate;
        this.createdAt = Instant.now();
    }
}
