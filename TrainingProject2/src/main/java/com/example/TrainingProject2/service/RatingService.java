package com.example.TrainingProject2.service;

import com.example.TrainingProject2.model.Appointment;
import com.example.TrainingProject2.model.Rating;
import com.example.TrainingProject2.model.User;
import com.example.TrainingProject2.repository.AppointmentRepository;
import com.example.TrainingProject2.repository.RatingRepository;
import com.example.TrainingProject2.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RatingService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }


    public Optional<Rating> getRatingForAppointment(Long appointmentId) {
        logger.info("Fetching rating for appointment ID: {}", appointmentId);
        return ratingRepository.findByAppointmentId(appointmentId);
    }

    public Rating saveRating(Long appointmentId, String username, int ratingScore, String comment) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));
        if (!appointment.getCustomer().equals(customer)) {
            logger.warn("User {} attempted to rate appointment {} which does not belong to them.", username, appointmentId);
            throw new SecurityException("Bạn không có quyền đánh giá lịch hẹn này.");
        }
        if (appointment.getStatus() != Appointment.Status.da_hoan_thanh) {
            logger.error("Appointment ID: {} is not completed, cannot be rated.", appointmentId);
            throw new IllegalArgumentException("Chỉ có thể đánh giá lịch hẹn đã hoàn thành.");
        }

        if (appointment.getRating() != null) {
            logger.warn("Appointment ID: {} has already been rated.", appointmentId);
            throw new IllegalArgumentException("Lịch hẹn này đã được đánh giá rồi.");
        }

        if (ratingScore < 1 || ratingScore > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5.");
        }
        Rating newRating = new Rating();
        newRating.setAppointment(appointment);
        newRating.setUser(customer);
        newRating.setRating(ratingScore);
        newRating.setComment(comment);

        return ratingRepository.save(newRating);

    }

    public Page<Rating> getAllRatings(Pageable pageable) {
        logger.info("Fetching all ratings for page {} with size {}", pageable.getPageNumber(), pageable.getPageSize());
        return ratingRepository.findAll(pageable);
    }

    public Optional<Rating> getRatingById(Long ratingId) {
        return ratingRepository.findById(ratingId);
    }

    @Transactional
    public void deleteRating(Long ratingId) {
        logger.info("Attempting to delete rating with ID: {}", ratingId);
        if (!ratingRepository.existsById(ratingId)) {
            logger.warn("Rating with ID {} not found for deletion.", ratingId);
            throw new IllegalArgumentException("Đánh giá không tồn tại.");
        }
        ratingRepository.deleteById(ratingId);
        logger.info("Rating with ID {} deleted successfully.", ratingId);
    }

    public Page<Rating> searchRatings(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllRatings(pageable);
        }
        logger.info("Searching ratings for term: '{}', page: {}, size: {}", searchTerm, pageable.getPageNumber(), pageable.getPageSize());
        return ratingRepository.findByAppointmentService_NameContainingIgnoreCaseOrAppointmentTechnician_NameContainingIgnoreCase(searchTerm, pageable);
    }

}
