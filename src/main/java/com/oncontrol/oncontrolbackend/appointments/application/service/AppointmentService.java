package com.oncontrol.oncontrolbackend.appointments.application.service;

import com.oncontrol.oncontrolbackend.appointments.application.dto.CreateAppointmentRequest;
import com.oncontrol.oncontrolbackend.appointments.application.dto.AppointmentResponse;
import com.oncontrol.oncontrolbackend.appointments.domain.model.Appointment;
import com.oncontrol.oncontrolbackend.appointments.domain.model.AppointmentStatus;
import com.oncontrol.oncontrolbackend.appointments.domain.repository.AppointmentRepository;
import com.oncontrol.oncontrolbackend.notifications.application.AppointmentNotificationService;
import com.oncontrol.oncontrolbackend.notifications.domain.AppointmentNotification;
import com.oncontrol.oncontrolbackend.profiles.domain.model.Profile;
import com.oncontrol.oncontrolbackend.profiles.domain.model.ProfileType;
import com.oncontrol.oncontrolbackend.profiles.domain.repository.PatientProfileRepository;
import com.oncontrol.oncontrolbackend.profiles.domain.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentService {
    private static final Set<AppointmentStatus> BOOKED = Set.of(
            AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED,
            AppointmentStatus.IN_PROGRESS, AppointmentStatus.RESCHEDULED);
    private static final Set<AppointmentStatus> CHANGEABLE = Set.of(
            AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.RESCHEDULED);

    private final AppointmentRepository appointmentRepository;
    private final ProfileRepository profileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AppointmentNotificationService notifications;

    /**
     * Create appointment (can be called by doctor or patient)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse createAppointment(Profile actor, Long doctorProfileId, Long patientProfileId, CreateAppointmentRequest request) {
        log.info("Creating appointment between doctor {} and patient {}", doctorProfileId, patientProfileId);

        requireParticipant(actor, doctorProfileId, patientProfileId);
        Profile doctorProfile = lockDoctor(doctorProfileId);

        Profile patientProfile = profileRepository.findById(patientProfileId)
                .filter(profile -> profile.getProfileType() == ProfileType.PATIENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));

        if (!patientProfileRepository.existsByProfile_IdAndDoctorProfile_Profile_Id(patientProfileId, doctorProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El paciente no está asignado a este médico");
        }

        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 30;
        if (duration < 1 || duration > 1440) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La duración de la cita debe estar entre 1 y 1440 minutos");
        }
        ensureAvailable(doctorProfileId, patientProfileId, 0L, request.getAppointmentDate(), duration);

        Appointment appointment = Appointment.builder()
                .doctor(doctorProfile)
                .patient(patientProfile)
                .appointmentDate(request.getAppointmentDate())
                .durationMinutes(duration)
                .type(request.getType())
                .status(AppointmentStatus.SCHEDULED)
                .location(request.getLocation())
                .notes(request.getNotes())
                .preparationInstructions(request.getPreparationInstructions())
                .sendReminder(request.getSendReminder() != null ? request.getSendReminder() : true)
                .build();

        appointment = appointmentRepository.save(appointment);
        if (actor.getProfileType() == ProfileType.DOCTOR) {
            notifications.record(patientProfileId, appointment, AppointmentNotification.Type.SCHEDULED);
        }

        log.info("Appointment created with ID: {}", appointment.getId());
        return mapToAppointmentResponse(appointment);
    }

    /**
     * Get all appointments for a doctor
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorProfileId) {
        return appointmentRepository.findByDoctorId(doctorProfileId).stream()
                .map(this::mapToAppointmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all appointments for a patient
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(Long patientProfileId) {
        return appointmentRepository.findByPatientId(patientProfileId).stream()
                .map(this::mapToAppointmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get appointment by ID
     */
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Profile actor, Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        requireParticipant(actor, appointment.getDoctor().getId(), appointment.getPatient().getId());
        return mapToAppointmentResponse(appointment);
    }

    /**
     * Update appointment status
     */
    public AppointmentResponse updateAppointmentStatus(Profile actor, Long id, AppointmentStatus status, String reason) {
        Appointment appointment = findForUpdate(id);
        requireParticipant(actor, appointment.getDoctor().getId(), appointment.getPatient().getId());

        if (actor.getProfileType() == ProfileType.PATIENT && status != AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (appointment.getStatus() == status) {
            return mapToAppointmentResponse(appointment);
        }
        if (status == AppointmentStatus.CANCELLED && !CHANGEABLE.contains(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita ya no se puede cancelar");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita ya está cancelada");
        }

        appointment.setStatus(status);
        
        if (status == AppointmentStatus.CANCELLED) {
            appointment.setCancellationReason(reason);
            appointment.setCancelledAt(LocalDateTime.now(ZoneOffset.UTC));
        } else if (status == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        }

        appointment = appointmentRepository.save(appointment);
        if (status == AppointmentStatus.CANCELLED) {
            Long recipient = actor.getProfileType() == ProfileType.PATIENT
                    ? appointment.getDoctor().getId() : appointment.getPatient().getId();
            notifications.record(recipient, appointment, AppointmentNotification.Type.CANCELLED);
        }
        return mapToAppointmentResponse(appointment);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse reschedule(Profile actor, Long id, LocalDateTime newDate) {
        Appointment appointment = findForUpdate(id);
        Long doctorId = appointment.getDoctor().getId();
        Long patientId = appointment.getPatient().getId();
        requireParticipant(actor, doctorId, patientId);
        if (actor.getProfileType() != ProfileType.PATIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!CHANGEABLE.contains(appointment.getStatus()) || !appointment.getAppointmentDate().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita ya no se puede reprogramar");
        }
        if (appointment.getAppointmentDate().equals(newDate)) {
            return mapToAppointmentResponse(appointment);
        }
        if (newDate.isBefore(now.plusHours(24))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una fecha con al menos 24 horas de anticipación");
        }

        lockDoctor(doctorId);
        int duration = appointment.getDurationMinutes();
        if (isOccupied(doctorId, patientId, id, newDate, duration)) {
            List<Instant> alternatives = IntStream.rangeClosed(1, 7)
                    .mapToObj(newDate::plusDays)
                    .filter(date -> !isOccupied(doctorId, patientId, id, date, duration))
                    .limit(3)
                    .map(date -> date.toInstant(ZoneOffset.UTC))
                    .toList();
            throw new ScheduleConflict(alternatives);
        }
        appointment.setAppointmentDate(newDate);
        appointment.setReminderSent(false);
        notifications.record(doctorId, appointment, AppointmentNotification.Type.RESCHEDULED);
        return mapToAppointmentResponse(appointment);
    }

    /**
     * Add follow-up notes to completed appointment
     */
    public AppointmentResponse addFollowUpNotes(Profile actor, Long id, String notes) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        requireOwnProfile(actor, ProfileType.DOCTOR, appointment.getDoctor().getId());

        appointment.setFollowUpNotes(notes);
        appointment = appointmentRepository.save(appointment);
        return mapToAppointmentResponse(appointment);
    }

    public void requireOwnProfile(Profile actor, ProfileType role, Long id) {
        if (actor == null || !actor.isEnabled() || actor.getProfileType() != role || !id.equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireParticipant(Profile actor, Long doctorId, Long patientId) {
        if (actor != null && actor.getProfileType() == ProfileType.DOCTOR) {
            requireOwnProfile(actor, ProfileType.DOCTOR, doctorId);
        } else {
            requireOwnProfile(actor, ProfileType.PATIENT, patientId);
        }
    }

    private Appointment findForUpdate(Long id) {
        return appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
    }

    private Profile lockDoctor(Long id) {
        return profileRepository.findByIdForScheduling(id)
                .filter(profile -> profile.getProfileType() == ProfileType.DOCTOR)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico no encontrado"));
    }

    private void ensureAvailable(Long doctorId, Long patientId, Long excludedId, LocalDateTime start, int duration) {
        if (isOccupied(doctorId, patientId, excludedId, start, duration)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese horario está ocupado. Elige otra fecha u hora");
        }
    }

    private boolean isOccupied(Long doctorId, Long patientId, Long excludedId, LocalDateTime start, int duration) {
        return appointmentRepository.hasOverlap(doctorId, patientId, excludedId, BOOKED, start, start.plusMinutes(duration));
    }

    @lombok.Getter
    public static class ScheduleConflict extends ResponseStatusException {
        private final List<Instant> alternatives;

        ScheduleConflict(List<Instant> alternatives) {
            super(HttpStatus.CONFLICT, "Ese horario está ocupado. Elige otra fecha u hora");
            this.alternatives = List.copyOf(alternatives);
        }
    }

    private AppointmentResponse mapToAppointmentResponse(Appointment appointment) {
        Profile doctor = appointment.getDoctor();
        Profile patient = appointment.getPatient();

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .doctorId(doctor.getId())
                .doctorName(doctor.getFullName())
                .patientId(patient.getId())
                .patientName(patient.getFullName())
                .appointmentDate(appointment.getAppointmentDate())
                .durationMinutes(appointment.getDurationMinutes())
                .type(appointment.getType())
                .status(appointment.getStatus())
                .location(appointment.getLocation())
                .notes(appointment.getNotes())
                .preparationInstructions(appointment.getPreparationInstructions())
                .followUpNotes(appointment.getFollowUpNotes())
                .cancellationReason(appointment.getCancellationReason())
                .build();
    }
}

