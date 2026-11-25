package com.oncontrol.oncontrolbackend.dashboard.infrastructure.controller;

import com.oncontrol.oncontrolbackend.dashboard.application.dto.*;
import com.oncontrol.oncontrolbackend.dashboard.application.service.DashboardService;
import com.oncontrol.oncontrolbackend.iam.domain.model.User;
import com.oncontrol.oncontrolbackend.profiles.domain.model.Profile;
import com.oncontrol.oncontrolbackend.profiles.domain.model.ProfileType;
import com.oncontrol.oncontrolbackend.profiles.domain.repository.DoctorProfileRepository;
import com.oncontrol.oncontrolbackend.profiles.domain.repository.PatientProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * DashboardController - Centralized dashboards with filtering capabilities
 * 
 * Features:
 * - Organization Dashboard: General view + Filter by specific doctor
 * - Doctor Dashboard: General view + Filter by specific patient  
 * - Patient Dashboard: Complete view of personal data
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard endpoints with filtering capabilities")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DoctorProfileRepository doctors;
    private final PatientProfileRepository patients;

    // ========== ORGANIZATION DASHBOARDS ==========

    @GetMapping("/organization/{organizationId}")
    @Operation(
        summary = "Get Organization Dashboard", 
        description = "Get complete dashboard for organization with all doctors and patients statistics"
    )
    public ResponseEntity<?> getOrganizationDashboard(
            @AuthenticationPrincipal User actor,
            @Parameter(description = "Organization ID") 
            @PathVariable Long organizationId) {
        requireOrganization(actor, organizationId);
        try {
            OrganizationDashboardResponse dashboard = dashboardService.getOrganizationDashboard(organizationId, null);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error retrieving organization dashboard", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving dashboard: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/organization/{organizationId}/filter/doctor/{doctorId}")
    @Operation(
        summary = "Get Organization Dashboard Filtered by Doctor",
        description = "Get organization dashboard filtered to show data only for a specific doctor"
    )
    public ResponseEntity<?> getOrganizationDashboardFilteredByDoctor(
            @AuthenticationPrincipal User actor,
            @Parameter(description = "Organization ID") 
            @PathVariable Long organizationId,
            @Parameter(description = "Doctor ID to filter by") 
            @PathVariable Long doctorId) {
        requireOrganization(actor, organizationId);
        try {
            OrganizationDashboardResponse dashboard = dashboardService.getOrganizationDashboard(organizationId, doctorId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error retrieving filtered organization dashboard", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving dashboard: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ========== DOCTOR DASHBOARDS ==========

    @GetMapping("/doctor/{doctorProfileId}")
    @Operation(
        summary = "Get Doctor Dashboard",
        description = "Get complete dashboard for doctor with all patients, appointments, and symptoms"
    )
    public ResponseEntity<?> getDoctorDashboard(
            @AuthenticationPrincipal Profile actor,
            @Parameter(description = "Doctor Profile ID") 
            @PathVariable Long doctorProfileId) {
        requireDoctor(actor, doctorProfileId);
        try {
            DoctorDashboardResponse dashboard = dashboardService.getDoctorDashboard(doctorProfileId, null);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error retrieving doctor dashboard", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving dashboard: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/doctor/{doctorProfileId}/filter/patient/{patientId}")
    @Operation(
        summary = "Get Doctor Dashboard Filtered by Patient",
        description = "Get doctor dashboard filtered to show data only for a specific patient"
    )
    public ResponseEntity<?> getDoctorDashboardFilteredByPatient(
            @AuthenticationPrincipal Profile actor,
            @Parameter(description = "Doctor Profile ID") 
            @PathVariable Long doctorProfileId,
            @Parameter(description = "Patient ID to filter by") 
            @PathVariable Long patientId) {
        requireDoctor(actor, doctorProfileId);
        try {
            DoctorDashboardResponse dashboard = dashboardService.getDoctorDashboard(doctorProfileId, patientId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error retrieving filtered doctor dashboard", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving dashboard: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ========== PATIENT DASHBOARDS ==========

    @GetMapping("/patient/{patientProfileId}")
    @Operation(
        summary = "Get Patient Dashboard",
        description = "Get complete dashboard for patient with appointments, symptoms, and statistics"
    )
    public ResponseEntity<?> getPatientDashboard(
            @AuthenticationPrincipal Profile actor,
            @Parameter(description = "Patient Profile ID") 
            @PathVariable Long patientProfileId) {
        requirePatient(actor, patientProfileId);
        try {
            PatientDashboardResponse dashboard = dashboardService.getPatientDashboard(patientProfileId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error retrieving patient dashboard", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving dashboard: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ========== STATISTICS ENDPOINTS ==========

    @GetMapping("/organization/{organizationId}/stats")
    @Operation(
        summary = "Get Organization Statistics",
        description = "Get quick statistics for organization"
    )
    public ResponseEntity<?> getOrganizationStats(@AuthenticationPrincipal User actor, @PathVariable Long organizationId) {
        requireOrganization(actor, organizationId);
        try {
            OrganizationDashboardResponse dashboard = dashboardService.getOrganizationDashboard(organizationId, null);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDoctors", dashboard.getTotalDoctors());
            stats.put("activeDoctors", dashboard.getActiveDoctors());
            stats.put("totalPatients", dashboard.getTotalPatients());
            stats.put("activePatients", dashboard.getActivePatients());
            stats.put("totalAppointments", dashboard.getTotalAppointments());
            stats.put("upcomingAppointments", dashboard.getUpcomingAppointments());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving organization stats", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving stats: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/doctor/{doctorProfileId}/stats")
    @Operation(
        summary = "Get Doctor Statistics",
        description = "Get quick statistics for doctor"
    )
    public ResponseEntity<?> getDoctorStats(@AuthenticationPrincipal Profile actor, @PathVariable Long doctorProfileId) {
        requireDoctor(actor, doctorProfileId);
        try {
            DoctorDashboardResponse dashboard = dashboardService.getDoctorDashboard(doctorProfileId, null);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPatients", dashboard.getTotalPatients());
            stats.put("activePatients", dashboard.getActivePatients());
            stats.put("totalAppointments", dashboard.getTotalAppointments());
            stats.put("upcomingAppointments", dashboard.getUpcomingAppointments());
            stats.put("completedAppointments", dashboard.getCompletedAppointments());
            stats.put("totalSymptomsReported", dashboard.getTotalSymptomsReported());
            stats.put("criticalSymptoms", dashboard.getCriticalSymptoms());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving doctor stats", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving stats: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/patient/{patientProfileId}/stats")
    @Operation(
        summary = "Get Patient Statistics",
        description = "Get quick statistics for patient"
    )
    public ResponseEntity<?> getPatientStats(@AuthenticationPrincipal Profile actor, @PathVariable Long patientProfileId) {
        requirePatient(actor, patientProfileId);
        try {
            PatientDashboardResponse dashboard = dashboardService.getPatientDashboard(patientProfileId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAppointments", dashboard.getTotalAppointments());
            stats.put("upcomingAppointments", dashboard.getUpcomingAppointments());
            stats.put("totalSymptoms", dashboard.getTotalSymptoms());
            stats.put("symptomsLastWeek", dashboard.getSymptomsLastWeek());
            stats.put("symptomsLastMonth", dashboard.getSymptomsLastMonth());
            stats.put("criticalSymptoms", dashboard.getCriticalSymptoms());
            stats.put("symptomsBySeverity", dashboard.getSymptomsBySeverity());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving patient stats", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving stats: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private void requireOrganization(User actor, Long organizationId) {
        if (actor == null || !actor.isEnabled() || !organizationId.equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireDoctor(Profile actor, Long doctorId) {
        if (actor == null || !actor.isEnabled() || actor.getProfileType() != ProfileType.DOCTOR
                || !doctors.existsByIdAndProfile_Id(doctorId, actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requirePatient(Profile actor, Long patientId) {
        if (actor == null || !actor.isEnabled() || actor.getProfileType() != ProfileType.PATIENT
                || !patients.existsByIdAndProfile_Id(patientId, actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}

