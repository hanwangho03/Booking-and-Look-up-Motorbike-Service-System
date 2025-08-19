package com.example.TrainingProject2.controller;

import com.example.TrainingProject2.config.TimeSlotGenerator;
import com.example.TrainingProject2.model.Appointment;
import com.example.TrainingProject2.model.User;
import com.example.TrainingProject2.repository.UserRepository;
import com.example.TrainingProject2.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/technician")
public class TechnicianController {

    private static final Logger logger = LoggerFactory.getLogger(TechnicianController.class);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    public record TimeSlot(String label, String startTime, String endTime) {}

    private boolean hasTechnicianRole(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated access to technician endpoint");
            return false;
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElse(null);
        if (user == null || user.getRole() == null || user.getRole().getId() != 2) {
            logger.warn("User {} does not have ROLE_TECHNICIAN (id=2)", auth.getName());
            return false;
        }
        logger.info("User {} has ROLE_TECHNICIAN (id=2)", auth.getName());
        return true;
    }

    @GetMapping({"/appointments", "/appointments/schedule"})
    public String showTechnicianAppointments(
            @RequestParam(defaultValue = "0") int weekOffset,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasTechnicianRole(auth)) {
            return "error/403";
        }

        String username = auth.getName();
        User currentTechnician = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin kỹ thuật viên đang đăng nhập."));

        LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(weekOffset);
        LocalDate endOfWeek = startOfWeek.plusDays(5);

        LocalDateTime startDateTime = startOfWeek.atTime(0, 0);
        LocalDateTime endDateTime = endOfWeek.atTime(23, 59);

        List<Appointment> appointments = appointmentService.findAppointmentsForTechnicianByDateRange(
                currentTechnician, startDateTime, endDateTime
        );

        model.addAttribute("appointments", appointments);
        model.addAttribute("weekOffset", weekOffset);
        model.addAttribute("startOfWeek", startOfWeek);
        model.addAttribute("timeSlots", TimeSlotGenerator.generate());

        return "technician/appointments";
    }

    @PostMapping("/appointments/cancel")
    public String cancelAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam(value = "weekOffset", defaultValue = "0") int weekOffset,
            @RequestParam(value = "cancellationReason", required = false) String cancellationReason,
            RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasTechnicianRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "error/403";
        }

        String username = auth.getName();
        User currentTechnician = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin kỹ thuật viên đang đăng nhập."));

        try {
            Appointment appointment = appointmentService.getAppointmentById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));

            if (!appointment.getTechnician().getId().equals(currentTechnician.getId())) {
                throw new SecurityException("Bạn không có quyền hủy lịch hẹn này.");
            }

            logger.info("Technician {} cancelling appointment ID: {} with reason: {}", username, appointmentId, cancellationReason);
            appointmentService.cancelAppointment(appointmentId, cancellationReason != null ? cancellationReason : "Không có lý do cụ thể.");
            redirectAttributes.addFlashAttribute("success", "Hủy lịch hẹn thành công!");
        } catch (IllegalArgumentException | SecurityException e) {
            logger.error("Error cancelling appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("weekOffset", weekOffset);
        return "redirect:/technician/appointments";
    }

    @PostMapping("/appointments/complete")
    public String completeAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam(value = "weekOffset", defaultValue = "0") int weekOffset,
            RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasTechnicianRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "error/403";
        }

        String username = auth.getName();
        User currentTechnician = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin kỹ thuật viên đang đăng nhập."));

        try {
            Appointment appointment = appointmentService.getAppointmentById(appointmentId) // Cần thêm getAppointmentById vào service
                    .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));

            if (!appointment.getTechnician().getId().equals(currentTechnician.getId())) {
                throw new SecurityException("Bạn không có quyền hoàn thành lịch hẹn này.");
            }

            logger.info("Technician {} completing appointment ID: {}", username, appointmentId);
            appointmentService.completeAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("success", "Đánh dấu hoàn thành lịch hẹn thành công!");
        } catch (IllegalArgumentException | SecurityException e) {
            logger.error("Error completing appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("weekOffset", weekOffset);
        return "redirect:/technician/appointments";
    }
}