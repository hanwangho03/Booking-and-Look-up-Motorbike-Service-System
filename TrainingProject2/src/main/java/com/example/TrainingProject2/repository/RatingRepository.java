package com.example.TrainingProject2.repository;

import com.example.TrainingProject2.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByAppointmentId(Long appointmentId);

    Page<Rating> findAll(Pageable pageable);

    @Query("SELECT r FROM Rating r WHERE LOWER(r.appointment.service.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Rating> findByAppointmentService_NameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT r FROM Rating r WHERE LOWER(r.appointment.technician.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Rating> findByAppointmentTechnician_NameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT r FROM Rating r WHERE " +
            "LOWER(r.appointment.service.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.appointment.technician.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Rating> findByAppointmentService_NameContainingIgnoreCaseOrAppointmentTechnician_NameContainingIgnoreCase(
            @Param("searchTerm") String searchTerm, Pageable pageable);
}