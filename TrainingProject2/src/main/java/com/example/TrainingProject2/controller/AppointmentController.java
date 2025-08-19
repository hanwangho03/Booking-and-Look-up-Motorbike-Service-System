package com.example.TrainingProject2.controller;

import com.example.TrainingProject2.dto.user.LoggedUserDto;
import com.example.TrainingProject2.model.Appointment;
import com.example.TrainingProject2.model.Rating;
import com.example.TrainingProject2.model.ServiceFix;
import com.example.TrainingProject2.model.User;
import com.example.TrainingProject2.repository.AppointmentRepository;
import com.example.TrainingProject2.repository.ServiceRepository;
import com.example.TrainingProject2.repository.UserRepository;
import com.example.TrainingProject2.service.AppointmentService;
import com.example.TrainingProject2.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private RatingService ratingService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @GetMapping("/appointment")
    public String showAppointmentPage(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                String roleName = user.getRole().getName();
                LoggedUserDto loggedUserDto = new LoggedUserDto(
                        user.getUsername(),
                        roleName,
                        user.getPhoneNumber(),
                        user.getName()
                );
                session.setAttribute("loggedUser", loggedUserDto);
                model.addAttribute("currentUser", user);
                logger.info("User found in /appointment: {}, Role: {}", user.getUsername(), roleName);
            });
        } else {
            logger.warn("Unauthenticated access to /appointment");
        }

        model.addAttribute("services", serviceRepository.findAll());
        return "carofix/contact-2";
    }

    @PostMapping("/appointment")
    public String bookAppointment(
            @RequestParam("serviceId") Integer serviceId,
            @RequestParam("date") String date,
            @RequestParam("time") String time,
            Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
                logger.warn("Unauthenticated access to bookAppointment");
                model.addAttribute("errorMessage", "Bạn chưa đăng nhập vui lòng đăng nhập hoặc đăng ký để tiếp tục !");
                return "redirect:/login";
            }

            String username = authentication.getName();
            logger.info("Booking appointment for username: {}, serviceId: {}, date: {}, time: {}", username, serviceId, date, time);

            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime startTime = LocalDateTime.of(localDate, localTime);

            appointmentService.createAppointment(username, serviceId, startTime);

            model.addAttribute("successMessage", "Đặt lịch thành công! Vui lòng chờ xác nhận.");
            model.addAttribute("services", serviceRepository.findAll());
            return "carofix/booking";
        } catch (IllegalArgumentException e) {
            logger.error("Error booking appointment: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("services", serviceRepository.findAll());
            return "carofix/booking";
        } catch (Exception e) {
            logger.error("Unexpected error booking appointment: {}", e.getMessage());
            model.addAttribute("errorMessage", "Lỗi hệ thống. Vui lòng thử lại sau.");
            model.addAttribute("services", serviceRepository.findAll());
            return "carofix/booking";
        }
    }

    @GetMapping("/carofix/booking")
    public String showBookingPage(Model model) {
        model.addAttribute("services", serviceRepository.findAll());
        return "carofix/booking";
    }
    @GetMapping("/appointment/history")
    public String showAppointmentHistory(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Page<Appointment> appointmentPage = appointmentService.getAppointmentHistoryForCustomer(username, page, size);

        model.addAttribute("appointmentPage", appointmentPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", appointmentPage.getTotalPages());

        return "appointment/history";
    }

    @GetMapping("/appointment/{appointmentId}/rate")
    public String showRatingForm(@PathVariable Long appointmentId, Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated access to rating form for appointment ID: {}", appointmentId);
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để đánh giá.");
            return "redirect:/login";
        }

        String username = authentication.getName();
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));

            if (!appointment.getCustomer().getUsername().equals(username)) {
                logger.warn("User {} attempted to access rating form for appointment {} which does not belong to them.", username, appointmentId);
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đánh giá lịch hẹn này.");
                return "redirect:/appointment/history";
            }

            Optional<Rating> existingRating = ratingService.getRatingForAppointment(appointmentId);

            model.addAttribute("appointment", appointment);
            model.addAttribute("existingRating", existingRating.orElse(null));
            model.addAttribute("isRated", existingRating.isPresent());

            return "appointment/rating-form";
        } catch (IllegalArgumentException e) {
            logger.error("Error accessing rating form for appointment {}: {}", appointmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/appointment/history";
        } catch (Exception e) {
            logger.error("Unexpected error accessing rating form for appointment {}: {}", appointmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống khi tải form đánh giá.");
            return "redirect:/appointment/history";
        }
    }
    @PostMapping("/appointment/cancel")
    public String cancelCustomerAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam(value = "cancellationReason", required = false) String cancellationReason,
            RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Kiểm tra xem người dùng có phải là CUSTOMER không
        if (!hasCustomerRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "error/403";
        }

        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin người dùng đang đăng nhập."));

        try {
            Appointment appointment = appointmentService.getAppointmentById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));

            if (!appointment.getCustomer().getId().equals(currentUser.getId())) {
                throw new SecurityException("Bạn không có quyền hủy lịch hẹn này.");
            }

            if (appointment.getStatus() == Appointment.Status.da_hoan_thanh || appointment.getStatus() == Appointment.Status.da_huy) {
                throw new IllegalArgumentException("Không thể hủy lịch hẹn đã hoàn thành hoặc đã bị hủy.");
            }

            logger.info("Customer {} cancelling appointment ID: {} with reason: {}", username, appointmentId, cancellationReason);
            appointmentService.cancelAppointment(appointmentId, cancellationReason != null ? cancellationReason : "Không có lý do cụ thể.");
            redirectAttributes.addFlashAttribute("success", "Hủy lịch hẹn thành công!");
        } catch (IllegalArgumentException | SecurityException e) {
            logger.error("Error cancelling appointment for customer: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/appointment/history"; // Chuyển hướng về trang lịch sử đặt lịch
    }
    private boolean hasCustomerRole(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated access to technician endpoint");
            return false;
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElse(null);
        if (user == null || user.getRole() == null || user.getRole().getId() != 3) {
            return false;
        }
        return true;
    }
    @PostMapping("/appointment/{appointmentId}/rate")
    public String submitRating(
            @PathVariable Long appointmentId,
            @RequestParam("ratingScore") int ratingScore,
            @RequestParam("comment") String comment,
            RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated attempt to submit rating for appointment ID: {}", appointmentId);
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để gửi đánh giá.");
            return "redirect:/login";
        }

        String username = authentication.getName();
        try {
            ratingService.saveRating(appointmentId, username, ratingScore, comment);
            redirectAttributes.addFlashAttribute("success", "Đánh giá của bạn đã được gửi thành công!");
        } catch (IllegalArgumentException | SecurityException e) {
            logger.error("Error submitting rating for appointment {}: {}", appointmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/appointment/" + appointmentId + "/rate";
        } catch (Exception e) {
            logger.error("Unexpected error submitting rating for appointment {}: {}", appointmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống khi gửi đánh giá.");
            return "redirect:/appointment/" + appointmentId + "/rate";
        }
        return "redirect:/appointment/history";
    }
    @PostMapping("/appointment/reschedule")
    @ResponseBody
    public ResponseEntity<Map<String, String>> rescheduleAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam("newStartTime") String newStartTimeStr) {

        Map<String, String> response = new HashMap<>();
        try {
            LocalDateTime newStartTime = LocalDateTime.parse(newStartTimeStr, DATETIME_FORMATTER);
            logger.info("Customer rescheduling appointment ID: {} to new start time: {}", appointmentId, newStartTime);
            appointmentService.rescheduleAppointment(appointmentId, newStartTime);
            response.put("status", "success");
            response.put("message", "Thay đổi thời gian lịch hẹn thành công! Vui lòng chờ admin xác nhận.");
        } catch (IllegalArgumentException e) {
            logger.error("Error rescheduling appointment: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error rescheduling appointment: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Đã có lỗi xảy ra. Vui lòng thử lại.");
        }
        return ResponseEntity.ok(response);
    }
}