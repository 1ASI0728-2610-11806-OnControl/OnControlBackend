package com.oncontrol.oncontrolbackend.appointments.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oncontrol.oncontrolbackend.appointments.application.dto.CreateAppointmentRequest;
import com.oncontrol.oncontrolbackend.appointments.domain.model.AppointmentStatus;
import com.oncontrol.oncontrolbackend.appointments.domain.model.AppointmentType;
import com.oncontrol.oncontrolbackend.iam.domain.model.User;
import com.oncontrol.oncontrolbackend.iam.domain.model.UserRole;
import com.oncontrol.oncontrolbackend.iam.domain.repository.UserRepository;
import com.oncontrol.oncontrolbackend.profiles.domain.model.DoctorProfile;
import com.oncontrol.oncontrolbackend.profiles.domain.model.PatientProfile;
import com.oncontrol.oncontrolbackend.profiles.domain.model.Profile;
import com.oncontrol.oncontrolbackend.profiles.domain.model.ProfileType;
import com.oncontrol.oncontrolbackend.profiles.domain.repository.DoctorProfileRepository;
import com.oncontrol.oncontrolbackend.profiles.domain.repository.PatientProfileRepository;
import com.oncontrol.oncontrolbackend.profiles.domain.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el módulo de Appointments
 * Prueba el flujo completo desde el controlador hasta la base de datos
 */
@SpringBootTest(properties = "app.payments.demo.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Profile doctorProfile;
    private Profile patientProfile;
    private DoctorProfile doctorRecord;
    private PatientProfile patientRecord;

    @BeforeEach
    void setUp() {
        // Crear organización
        User organization = User.builder()
                .organizationName("Test Organization")
                .email("org@test.com")
                .password(passwordEncoder.encode("password123"))
                .country("Perú")
                .city("Lima")
                .role(UserRole.ORGANIZATION)
                .isActive(true)
                .build();
        organization = userRepository.save(organization);

        // Crear perfil de doctor
        doctorProfile = Profile.builder()
                .user(organization)
                .profileType(ProfileType.DOCTOR)
                .email("doctor@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Dr. Juan")
                .lastName("Pérez")
                .phone("987654321")
                .birthDate(LocalDate.of(1980, 1, 15))
                .city("Lima")
                .address("Av. Test 123")
                .isActive(true)
                .build();
        doctorProfile = profileRepository.save(doctorProfile);

        // Crear perfil específico de doctor
        DoctorProfile doctorSpecificProfile = DoctorProfile.builder()
                .profile(doctorProfile)
                .organization(organization)
                .specialization("Oncología")
                .licenseNumber("MED-12345")
                .yearsOfExperience(15)
                .hospitalAffiliation("Hospital Nacional")
                .consultationFee(new BigDecimal("150.00"))
                .bio("Especialista en oncología con 15 años de experiencia")
                .isAvailable(true)
                .rating(4.5)
                .totalReviews(50)
                .build();
        doctorRecord = doctorProfileRepository.save(doctorSpecificProfile);

        // Crear perfil de paciente
        patientProfile = Profile.builder()
                .user(organization)
                .profileType(ProfileType.PATIENT)
                .email("patient@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("María")
                .lastName("González")
                .phone("912345678")
                .birthDate(LocalDate.of(1990, 5, 20))
                .city("Lima")
                .address("Jr. Test 456")
                .isActive(true)
                .build();
        patientProfile = profileRepository.save(patientProfile);

        // Crear perfil específico de paciente
        PatientProfile patientSpecificProfile = PatientProfile.builder()
                .profile(patientProfile)
                .doctorProfile(doctorSpecificProfile)
                .bloodType("O+")
                .emergencyContactName("Juan González")
                .emergencyContactPhone("987654321")
                .insuranceProvider("Seguro Test")
                .insuranceNumber("INS-12345")
                .build();
        patientRecord = patientProfileRepository.save(patientSpecificProfile);
    }

    @Test
    @DisplayName("Debe crear una cita exitosamente")
    void testCreateAppointment_Success() throws Exception {
        // Preparar datos
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .appointmentDate(LocalDateTime.now().plusDays(7))
                .durationMinutes(30)
                .type(AppointmentType.PRIMERA_CONSULTA)
                .location("Consultorio 101")
                .notes("Primera consulta de control")
                .preparationInstructions("Traer exámenes previos")
                .sendReminder(true)
                .build();

        // Ejecutar y verificar
        mockMvc.perform(post("/api/appointments/doctor/{doctorId}/patient/{patientId}",
                        doctorProfile.getId(), patientProfile.getId())
                        .with(user(doctorProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Appointment created successfully"))
                .andExpect(jsonPath("$.appointment.id").exists())
                .andExpect(jsonPath("$.appointment.type").value("PRIMERA_CONSULTA"))
                .andExpect(jsonPath("$.appointment.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.appointment.location").value("Consultorio 101"))
                .andExpect(jsonPath("$.appointment.durationMinutes").value(30));
    }

    @Test
    void dashboardCountsMatchAppointmentLifecycleForParticipants() throws Exception {
        var original = LocalDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0);
        var newDate = original.plusDays(1);
        Long id = createTestAppointment(original, AppointmentType.PRIMERA_CONSULTA);

        expectDashboardCounts(doctorDashboard(), 1, 1);
        expectDashboardCounts(patientDashboard(), 1, 1);
        mockMvc.perform(get("/api/dashboard/doctor/{id}", doctorRecord.getId()).with(user(patientProfile)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/appointments/{id}/schedule", id).with(user(patientProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("appointmentDate", newDate.toInstant(ZoneOffset.UTC)))))
                .andExpect(status().isOk());
        expectDashboardCounts(doctorDashboard(), 1, 1)
                .andExpect(jsonPath("$.upcomingAppointmentsList[0].id").value(id.intValue()))
                .andExpect(jsonPath("$.upcomingAppointmentsList[0].appointmentDate").value(newDate.toString()));
        expectDashboardCounts(patientDashboard(), 1, 1);

        mockMvc.perform(patch("/api/appointments/{id}/status", id).with(user(patientProfile))
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk());
        expectDashboardCounts(doctorDashboard(), 1, 0)
                .andExpect(jsonPath("$.upcomingAppointmentsList", hasSize(0)))
                .andExpect(jsonPath("$.patientStatistics." + patientRecord.getId() + ".upcomingAppointments").value(0));
        expectDashboardCounts(patientDashboard(), 1, 0);
    }

    private ResultActions doctorDashboard() throws Exception {
        return mockMvc.perform(get("/api/dashboard/doctor/{id}", doctorRecord.getId()).with(user(doctorProfile)));
    }

    private ResultActions patientDashboard() throws Exception {
        return mockMvc.perform(get("/api/dashboard/patient/{id}", patientRecord.getId()).with(user(patientProfile)));
    }

    private ResultActions expectDashboardCounts(ResultActions result, int total, int upcoming) throws Exception {
        return result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAppointments").value(total))
                .andExpect(jsonPath("$.upcomingAppointments").value(upcoming));
    }

    @Test
    void appointmentNoticesStayWithRecipientAndDoNotRepeat() throws Exception {
        var date = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
        Long appointmentId = createTestAppointment(date, AppointmentType.PRIMERA_CONSULTA);

        String inbox = mockMvc.perform(get("/api/notifications/me").with(user(patientProfile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].type").value("SCHEDULED"))
                .andExpect(jsonPath("$.items[0].appointmentId").value(appointmentId.intValue()))
                .andReturn().getResponse().getContentAsString();
        long notificationId = objectMapper.readTree(inbox).at("/items/0/id").asLong();
        mockMvc.perform(get("/api/notifications/me").with(user(doctorProfile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(put("/api/notifications/me/{id}/read", notificationId).with(user(patientProfile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isString());
        mockMvc.perform(get("/api/notifications/me").with(user(patientProfile)))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].readAt").isString());

        for (int retry = 0; retry < 2; retry++) {
            mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId).with(user(patientProfile))
                            .param("status", "CANCELLED"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/notifications/me").with(user(doctorProfile)))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type").value("CANCELLED"));
    }

    @Test
    void rescheduleRejectsOverlapAndNotifiesDoctorOnceWhenDateChanges() throws Exception {
        var original = LocalDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0);
        var occupied = original.plusDays(1);
        var target = original.plusDays(2);
        Long id = createTestAppointment(original, AppointmentType.PRIMERA_CONSULTA);
        createTestAppointment(occupied, AppointmentType.CONSULTA_SEGUIMIENTO);

        mockMvc.perform(patch("/api/appointments/{id}/schedule", id).with(user(patientProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("appointmentDate", occupied.plusMinutes(15).toInstant(ZoneOffset.UTC)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.alternatives", hasSize(3)));
        mockMvc.perform(get("/api/appointments/{id}", id).with(user(patientProfile)))
                .andExpect(jsonPath("$.appointmentDate").value(original.toString()));
        mockMvc.perform(get("/api/notifications/me").with(user(doctorProfile)))
                .andExpect(jsonPath("$.unreadCount").value(0));

        for (int retry = 0; retry < 2; retry++) {
            mockMvc.perform(patch("/api/appointments/{id}/schedule", id).with(user(patientProfile))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("appointmentDate", target.toInstant(ZoneOffset.UTC)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointment.appointmentDate").value(target.toString()));
        }
        mockMvc.perform(get("/api/appointments/doctor/{id}", doctorProfile.getId()).with(user(doctorProfile)))
                .andExpect(jsonPath("$.appointments[*].appointmentDate", hasItem(target.toString())));
        mockMvc.perform(get("/api/notifications/me").with(user(doctorProfile)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type").value("RESCHEDULED"))
                .andExpect(jsonPath("$.items[0].appointmentDate").value(target.toInstant(ZoneOffset.UTC).toString()));
    }

    @Test
    void demoPaymentStoresOnlyAcceptedScenariosAndCanRemoveThem() throws Exception {
        for (String invalid : new String[]{"DECLINED", "EXPIRED"}) {
            mockMvc.perform(post("/api/payment-methods/demo").with(user(patientProfile))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("scenario", invalid))))
                    .andExpect(status().isUnprocessableEntity());
        }
        mockMvc.perform(get("/api/payment-methods/demo").with(user(patientProfile)))
                .andExpect(jsonPath("$", hasSize(0)));

        String result = mockMvc.perform(post("/api/payment-methods/demo").with(user(patientProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("scenario", "VALID_VISA"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastFour").value("4242"))
                .andReturn().getResponse().getContentAsString();
        long methodId = objectMapper.readTree(result).get("id").asLong();
        mockMvc.perform(get("/api/payment-methods/demo").with(user(patientProfile)))
                .andExpect(jsonPath("$[0].brand").value("Visa"));
        mockMvc.perform(delete("/api/payment-methods/demo/{id}", methodId).with(user(patientProfile)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/payment-methods/demo").with(user(patientProfile)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Debe obtener todas las citas de un doctor")
    void testGetDoctorAppointments_Success() throws Exception {
        // Crear citas de prueba
        createTestAppointment(LocalDateTime.now().plusDays(1), AppointmentType.PRIMERA_CONSULTA);
        createTestAppointment(LocalDateTime.now().plusDays(2), AppointmentType.CONSULTA_SEGUIMIENTO);

        // Ejecutar y verificar
        mockMvc.perform(get("/api/appointments/doctor/{doctorId}", doctorProfile.getId())
                        .with(user(doctorProfile)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments", hasSize(2)))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @DisplayName("Debe obtener todas las citas de un paciente")
    void testGetPatientAppointments_Success() throws Exception {
        // Crear citas de prueba
        createTestAppointment(LocalDateTime.now().plusDays(1), AppointmentType.PRIMERA_CONSULTA);
        createTestAppointment(LocalDateTime.now().plusDays(3), AppointmentType.REVISION_TRATAMIENTO);

        // Ejecutar y verificar
        mockMvc.perform(get("/api/appointments/patient/{patientId}", patientProfile.getId())
                        .with(user(patientProfile)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments", hasSize(2)))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @DisplayName("Debe obtener una cita por su ID")
    void testGetAppointmentById_Success() throws Exception {
        // Crear cita de prueba
        Long appointmentId = createTestAppointment(
                LocalDateTime.now().plusDays(5), 
                AppointmentType.CONSULTA_SEGUIMIENTO
        );

        // Ejecutar y verificar
        mockMvc.perform(get("/api/appointments/{id}", appointmentId)
                        .with(user(doctorProfile)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId))
                .andExpect(jsonPath("$.type").value("CONSULTA_SEGUIMIENTO"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("Debe actualizar el estado de una cita")
    void testUpdateAppointmentStatus_Success() throws Exception {
        // Crear cita de prueba
        Long appointmentId = createTestAppointment(
                LocalDateTime.now().plusDays(1), 
                AppointmentType.PRIMERA_CONSULTA
        );

        // Ejecutar y verificar
        mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId)
                        .with(user(doctorProfile))
                        .param("status", AppointmentStatus.CONFIRMED.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Appointment status updated successfully"))
                .andExpect(jsonPath("$.appointment.id").value(appointmentId))
                .andExpect(jsonPath("$.appointment.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Debe cancelar una cita con razón")
    void testCancelAppointment_WithReason() throws Exception {
        // Crear cita de prueba
        Long appointmentId = createTestAppointment(
                LocalDateTime.now().plusDays(2), 
                AppointmentType.PRIMERA_CONSULTA
        );

        // Ejecutar y verificar
        mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId)
                        .with(user(doctorProfile))
                        .param("status", AppointmentStatus.CANCELLED.name())
                        .param("reason", "El paciente solicitó reprogramar"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Appointment status updated successfully"))
                .andExpect(jsonPath("$.appointment.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Debe agregar notas de seguimiento a una cita completada")
    void testAddFollowUpNotes_Success() throws Exception {
        // Crear y completar cita de prueba
        Long appointmentId = createTestAppointment(
                LocalDateTime.now().minusDays(1), 
                AppointmentType.PRIMERA_CONSULTA
        );

        // Completar la cita primero
        mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId)
                        .with(user(doctorProfile))
                        .param("status", AppointmentStatus.COMPLETED.name()))
                .andExpect(status().isOk());

        // Agregar notas de seguimiento
        String notesJson = objectMapper.writeValueAsString(
                java.util.Map.of("notes", "El paciente presenta mejoría significativa")
        );

        mockMvc.perform(patch("/api/appointments/{id}/follow-up", appointmentId)
                        .with(user(doctorProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow-up notes added successfully"))
                .andExpect(jsonPath("$.appointment.id").value(appointmentId));
    }

    @Test
    @DisplayName("Debe fallar al crear una cita sin fecha")
    void testCreateAppointment_MissingDate() throws Exception {
        // Preparar datos inválidos (sin fecha)
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .durationMinutes(30)
                .type(AppointmentType.PRIMERA_CONSULTA)
                .location("Consultorio 101")
                .build();

        // Ejecutar y verificar que falla la validación
        mockMvc.perform(post("/api/appointments/doctor/{doctorId}/patient/{patientId}",
                        doctorProfile.getId(), patientProfile.getId())
                        .with(user(doctorProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe fallar al obtener una cita con ID inexistente")
    void testGetAppointmentById_NotFound() throws Exception {
        Long nonExistentId = 99999L;

        mockMvc.perform(get("/api/appointments/{id}", nonExistentId)
                        .with(user(doctorProfile)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Método auxiliar para crear una cita de prueba
     */
    private Long createTestAppointment(LocalDateTime appointmentDate, AppointmentType type) throws Exception {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .appointmentDate(appointmentDate)
                .durationMinutes(30)
                .type(type)
                .location("Consultorio Test")
                .notes("Cita de prueba")
                .sendReminder(true)
                .build();

        String response = mockMvc.perform(post("/api/appointments/doctor/{doctorId}/patient/{patientId}",
                        doctorProfile.getId(), patientProfile.getId())
                        .with(user(doctorProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonResponse = objectMapper.readTree(response);
        return jsonResponse.get("appointment").get("id").asLong();
    }
}
