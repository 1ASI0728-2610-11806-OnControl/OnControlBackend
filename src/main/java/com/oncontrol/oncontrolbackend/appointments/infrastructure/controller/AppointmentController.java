package com.oncontrol.oncontrolbackend.appointments.infrastructure.controller;

import com.oncontrol.oncontrolbackend.appointments.application.dto.CreateAppointmentRequest;
import com.oncontrol.oncontrolbackend.appointments.application.dto.AppointmentResponse;
import com.oncontrol.oncontrolbackend.appointments.application.service.AppointmentService;
import com.oncontrol.oncontrolbackend.appointments.domain.model.AppointmentStatus;
import com.oncontrol.oncontrolbackend.profiles.domain.model.Profile;
import com.oncontrol.oncontrolbackend.profiles.domain.model.ProfileType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AppointmentController - Manage appointments between doctors and patients
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appointments", description = "Appointment management endpoints")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/doctor/{doctorProfileId}/patient/{patientProfileId}")
    @Operation(summary = "Create appointment", description = "Create an appointment between a doctor and patient")
    public ResponseEntity<?> createAppointment(
            @AuthenticationPrincipal Profile actor,
            @PathVariable Long doctorProfileId,
            @PathVariable Long patientProfileId,
            @Valid @RequestBody CreateAppointmentRequest request) {
        try {
            AppointmentResponse appointment = appointmentService.createAppointment(actor, doctorProfileId, patientProfileId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("appointment", appointment);
            response.put("message", "Appointment created successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating appointment", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error creating appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/doctor/{doctorProfileId}")
    @Operation(summary = "Get doctor appointments", description = "Get all appointments for a doctor")
    public ResponseEntity<?> getDoctorAppointments(@AuthenticationPrincipal Profile actor, @PathVariable Long doctorProfileId) {
        appointmentService.requireOwnProfile(actor, ProfileType.DOCTOR, doctorProfileId);
        try {
            List<AppointmentResponse> appointments = appointmentService.getAppointmentsByDoctor(doctorProfileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("appointments", appointments);
            response.put("count", appointments.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving doctor appointments", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving appointments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/patient/{patientProfileId}")
    @Operation(summary = "Get patient appointments", description = "Get all appointments for a patient")
    public ResponseEntity<?> getPatientAppointments(@AuthenticationPrincipal Profile actor, @PathVariable Long patientProfileId) {
        appointmentService.requireOwnProfile(actor, ProfileType.PATIENT, patientProfileId);
        try {
            List<AppointmentResponse> appointments = appointmentService.getAppointmentsByPatient(patientProfileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("appointments", appointments);
            response.put("count", appointments.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving patient appointments", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving appointments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID", description = "Get a specific appointment by ID")
    public ResponseEntity<?> getAppointmentById(@AuthenticationPrincipal Profile actor, @PathVariable Long id) {
        try {
            AppointmentResponse appointment = appointmentService.getAppointmentById(actor, id);
            return ResponseEntity.ok(appointment);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving appointment", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status", description = "Update the status of an appointment")
    public ResponseEntity<?> updateAppointmentStatus(
            @AuthenticationPrincipal Profile actor,
            @PathVariable Long id,
            @RequestParam AppointmentStatus status,
            @RequestParam(required = false) String reason) {
        try {
            AppointmentResponse appointment = appointmentService.updateAppointmentStatus(actor, id, status, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("appointment", appointment);
            response.put("message", "Appointment status updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating appointment status", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error updating status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PatchMapping("/{id}/schedule")
    public Map<String, AppointmentResponse> reschedule(
            @AuthenticationPrincipal Profile actor,
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request) {
        var date = LocalDateTime.ofInstant(request.appointmentDate(), ZoneOffset.UTC);
        return Map.of("appointment", appointmentService.reschedule(actor, id, date));
    }

    public record RescheduleRequest(@NotNull Instant appointmentDate) {}

    @PatchMapping("/{id}/follow-up")
    @Operation(summary = "Add follow-up notes", description = "Add follow-up notes to a completed appointment")
    public ResponseEntity<?> addFollowUpNotes(
            @AuthenticationPrincipal Profile actor,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String notes = body.get("notes");
            AppointmentResponse appointment = appointmentService.addFollowUpNotes(actor, id, notes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("appointment", appointment);
            response.put("message", "Follow-up notes added successfully");
            
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error adding follow-up notes", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error adding notes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleAppointmentError(ResponseStatusException exception) {
        String message = exception.getReason() != null ? exception.getReason() : "Acción no permitida";
        if (exception instanceof AppointmentService.ScheduleConflict conflict) {
            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of("message", message, "alternatives", conflict.getAlternatives()));
        }
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
    }
}

