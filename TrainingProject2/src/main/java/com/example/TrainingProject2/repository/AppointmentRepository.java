package com.example.TrainingProject2.repository;

import com.example.TrainingProject2.model.Appointment;
import com.example.TrainingProject2.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.status IN ('da_xac_nhan', 'cho_xac_nhan') " +
            "AND a.startTime = :startTime")
    List<Appointment> findAppointmentsAtTimeSlot(LocalDateTime startTime);

    List<Appointment> findByTechnicianAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            User technician, LocalDateTime endTime, LocalDateTime startTime);

    Optional<Appointment> findByTechnicianAndStartTime(User technician, LocalDateTime startTime);

    @Query("SELECT a FROM Appointment a " +
            "WHERE a.technician = :technician " +
            "AND a.status IN ('cho_xac_nhan', 'da_xac_nhan') " +
            "AND (" +
            "   (a.startTime < :endTime AND a.endTime > :startTime)" +
            ")")
    List<Appointment> findConflictingAppointmentsForTechnician(
            @Param("technician") User technician,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.startTime = :startTime " +
            "AND a.status IN ('cho_xac_nhan', 'da_xac_nhan')")
    long countActiveAppointmentsAtStartTime(@Param("startTime") LocalDateTime startTime);

    List<Appointment> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a.technician.name, COUNT(a.id) FROM Appointment a " +
            "WHERE FUNCTION('YEAR', a.startTime) = :year AND FUNCTION('MONTH', a.startTime) = :month " +
            "AND a.status IN ('da_xac_nhan', 'da_hoan_thanh') " +
            "GROUP BY a.technician.name")
    List<Object[]> countTechnicianWorkloadPerMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT FUNCTION('YEAR', a.startTime) as year, FUNCTION('WEEK', a.startTime) as week, COUNT(a.id) FROM Appointment a " +
            "WHERE a.startTime BETWEEN :startDate AND :endDate AND a.status IN ('da_xac_nhan', 'da_hoan_thanh') " +
            "GROUP BY FUNCTION('YEAR', a.startTime), FUNCTION('WEEK', a.startTime) ORDER BY year, week")
    List<Object[]> countAppointmentsPerWeek(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    @Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.technician = :technician " +
            "AND a.startTime BETWEEN :start AND :end " +
            "AND a.status != 'da_huy'")
    long countActiveAppointmentsByTechnicianAndStartTimeBetween(
            @Param("technician") User technician,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    Page<Appointment> findByCustomerOrderByStartTimeDesc(User customer, Pageable pageable);

    List<Appointment> findByTechnicianAndStartTimeBetween(User technician, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.startTime BETWEEN :start AND :end " +
            "AND (:serviceName IS NULL OR a.service.name = :serviceName) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:technicianId IS NULL OR a.technician.id = :technicianId)")
    List<Appointment> findAppointmentsWithFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("serviceName") String serviceName,
            @Param("status") Appointment.Status status,
            @Param("technicianId") Long technicianId);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND (a.startTime + 8 HOUR) < :currentTime")
    List<Appointment> findPendingAppointmentsToAutoCancelByStartTime(@Param("status") Appointment.Status status,
                                                                     @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.customer.id = :customerId and DATE(a.startTime) = :date AND a.status IN ('cho_xac_nhan', 'da_xac_nhan')")
    int countAppointmentsByCustomerAndDay(@Param("customerId") Integer customerId, @Param("date") LocalDate date);

}