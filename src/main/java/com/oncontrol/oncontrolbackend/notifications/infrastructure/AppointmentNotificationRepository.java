package com.oncontrol.oncontrolbackend.notifications.infrastructure;

import com.oncontrol.oncontrolbackend.notifications.domain.AppointmentNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentNotificationRepository extends JpaRepository<AppointmentNotification, Long> {
    List<AppointmentNotification> findByRecipientIdOrderByIdDesc(Long recipientId, Pageable page);
    List<AppointmentNotification> findByRecipientIdAndIdLessThanOrderByIdDesc(Long recipientId, Long beforeId, Pageable page);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
    Optional<AppointmentNotification> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppointmentNotification n set n.readAt = :readAt where n.id = :id and n.recipientId = :recipientId and n.readAt is null")
    void markRead(Long id, Long recipientId, Instant readAt);
}
