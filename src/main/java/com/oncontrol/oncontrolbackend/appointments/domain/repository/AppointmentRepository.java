package com.oncontrol.oncontrolbackend.appointments.domain.repository;

import com.oncontrol.oncontrolbackend.appointments.domain.model.Appointment;
import com.oncontrol.oncontrolbackend.appointments.domain.model.AppointmentStatus;
import com.oncontrol.oncontrolbackend.appointments.domain.model.AppointmentType;
import com.oncontrol.oncontrolbackend.profiles.domain.model.Profile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(Long id);

    @Query("""
            select count(a) > 0 from Appointment a
            where (a.doctor.id = :doctorId or a.patient.id = :patientId) and a.id <> :excludedId
              and a.status in :activeStatuses and a.appointmentDate < :endTime
              and timestampadd(minute, a.durationMinutes, a.appointmentDate) > :startTime
            """)
    boolean hasOverlap(Long doctorId, Long patientId, Long excludedId,
                       Set<AppointmentStatus> activeStatuses, LocalDateTime startTime, LocalDateTime endTime);
    
    List<Appointment> findByDoctorId(Long doctorId);
    
    List<Appointment> findByPatientId(Long patientId);
    
    List<Appointment> findByDoctorAndAppointmentDateBetween(Profile doctor, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Appointment> findByPatientAndAppointmentDateBetween(Profile patient, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Appointment> findByDoctorAndStatus(Profile doctor, AppointmentStatus status);
    
    List<Appointment> findByPatientAndStatus(Profile patient, AppointmentStatus status);
    
    List<Appointment> findByAppointmentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Appointment> findByAppointmentDateAndDoctor(LocalDateTime appointmentDate, Profile doctor);
    
    @Query("SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.appointmentDate >= :startDate ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingByDoctor(@Param("doctor") Profile doctor, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT a FROM Appointment a WHERE a.patient = :patient AND a.appointmentDate >= :startDate ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingByPatient(@Param("patient") Profile patient, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor = :doctor AND a.appointmentDate BETWEEN :startDate AND :endDate")
    Long countByDoctorAndDateRange(@Param("doctor") Profile doctor, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM Appointment a WHERE a.type = :type AND a.status = :status")
    List<Appointment> findByTypeAndStatus(@Param("type") AppointmentType type, @Param("status") AppointmentStatus status);
}
