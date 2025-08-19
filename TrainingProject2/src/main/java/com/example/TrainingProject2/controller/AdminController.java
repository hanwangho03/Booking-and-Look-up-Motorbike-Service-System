package com.example.TrainingProject2.controller;

import com.example.TrainingProject2.config.TimeSlotGenerator;
import com.example.TrainingProject2.model.Appointment;
import com.example.TrainingProject2.model.Rating;
import com.example.TrainingProject2.model.ServiceFix;
import com.example.TrainingProject2.model.User;
import com.example.TrainingProject2.repository.UserRepository;
import com.example.TrainingProject2.service.AppointmentService;
import com.example.TrainingProject2.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private RatingService ratingService;

    @Autowired
    private UserRepository userRepository;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public record TimeSlot(String label, String startTime, String endTime) {}

    private boolean hasAdminRole(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            logger.warn("Unauthenticated access to admin endpoint");
            return false;
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElse(null);
        if (user == null || user.getRole() == null || user.getRole().getId() != 1) {
            logger.warn("User {} does not have ROLE_ADMIN (id=1)", auth.getName());
            return false;
        }
        logger.info("User {} has ROLE_ADMIN (id=1)", auth.getName());
        return true;
    }

    @GetMapping({"/appointments", "/appointments/schedule"})
    public String showAppointmentsWithSchedule(
            @RequestParam(defaultValue = "0") int weekOffset,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long technicianId,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            return "error/403";
        }

        LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(weekOffset);
        LocalDate endOfWeek = startOfWeek.plusDays(5);

        LocalDateTime startDateTime = startOfWeek.atTime(0, 0);
        LocalDateTime endDateTime = endOfWeek.atTime(23, 59);

        String actualServiceName = (serviceName != null && !serviceName.isEmpty()) ? serviceName : null;

        Appointment.Status actualStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                actualStatus = Appointment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status parameter received: " + status, e);
            }
        }

        List<Appointment> appointments = appointmentService.findAppointmentsByDateRangeAndFilters(
                startDateTime, endDateTime, actualServiceName, actualStatus, technicianId
        );

        List<ServiceFix> services = appointmentService.getAllServices();
        List<User> technicians = appointmentService.getAllTechnicians();
        List<Appointment.Status> allStatuses = appointmentService.getAllAppointmentStatuses();

        model.addAttribute("appointments", appointments);
        model.addAttribute("weekOffset", weekOffset);
        model.addAttribute("startOfWeek", startOfWeek);
        model.addAttribute("timeSlots", TimeSlotGenerator.generate());

        model.addAttribute("services", services);
        model.addAttribute("technicians", technicians);
        model.addAttribute("statuses", allStatuses);

        model.addAttribute("selectedServiceName", actualServiceName);
        model.addAttribute("selectedStatus", actualStatus);
        model.addAttribute("selectedTechnicianId", technicianId);

        return "admin/appointments";
    }


    @PostMapping("/appointments/approve")
    public String approveAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam(value = "weekOffset", defaultValue = "0") int weekOffset,
            RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "error/403";
        }
        try {
            logger.info("Admin approving appointment ID: {}", appointmentId);
            appointmentService.approveAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("success", "Phê duyệt lịch hẹn thành công!");
        } catch (IllegalArgumentException e) {
            logger.error("Error approving appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("weekOffset", weekOffset);
        return "redirect:/admin/appointments";
    }

    @PostMapping("/appointments/cancel")
    public String cancelAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam(value = "weekOffset", defaultValue = "0") int weekOffset,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "technicianIdFilter", required = false) Long technicianIdFilter,
            @RequestParam(value = "cancellationReason", required = false) String cancellationReason,
            RedirectAttributes redirectAttributes) {

        Logger logger = LoggerFactory.getLogger(AdminController.class);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "error/403";
        }
        try {
            logger.info("Admin cancelling appointment ID: {} with reason: {}", appointmentId, cancellationReason);
            // TRUYỀN THÊM LÝ DO HỦY VÀO SERVICE
            appointmentService.cancelAppointment(appointmentId, cancellationReason != null ? cancellationReason : "Không có lý do cụ thể.");
            redirectAttributes.addFlashAttribute("success", "Hủy lịch hẹn thành công!");
        } catch (IllegalArgumentException e) {
            logger.error("Error cancelling appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        // Giữ lại các tham số lọc khi redirect
        redirectAttributes.addAttribute("weekOffset", weekOffset);
        redirectAttributes.addAttribute("serviceName", serviceName);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("technicianId", technicianIdFilter);
        return "redirect:/admin/appointments/schedule"; // Redirect về trang schedule
    }


    @PostMapping("/appointments/complete")
    public String completeAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam(value = "weekOffset", defaultValue = "0") int weekOffset,
            RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "error/403";
        }

        try {
            logger.info("Admin completing appointment ID: {}", appointmentId);
            appointmentService.completeAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("success", "Đánh dấu hoàn thành lịch hẹn thành công!");
        } catch (IllegalArgumentException e) {
            logger.error("Error completing appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("weekOffset", weekOffset);
        return "redirect:/admin/appointments";
    }
    @GetMapping("/statistics")
    public String showStatistics(@RequestParam(defaultValue = "0") int monthOffset, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "redirect:/error/403";
        }

        LocalDate currentMonth = LocalDate.now().plusMonths(monthOffset);
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        Map<String, Long> weeklyStats = appointmentService.getWeeklyAppointmentStatistics(year, month);
        List<String> weeklyLabels = weeklyStats.keySet().stream().collect(Collectors.toList());
        List<Long> weeklyData = weeklyStats.values().stream().collect(Collectors.toList());

        Map<String, Long> technicianWorkload = appointmentService.getTechnicianWorkloadStatistics(year, month);
        List<String> technicianNames = technicianWorkload.keySet().stream().collect(Collectors.toList());
        List<Long> workloadCounts = technicianWorkload.values().stream().collect(Collectors.toList());

        model.addAttribute("weeklyLabels", weeklyLabels);
        model.addAttribute("weeklyData", weeklyData);
        model.addAttribute("technicianNames", technicianNames);
        model.addAttribute("workloadCounts", workloadCounts);
        model.addAttribute("currentMonthYear", currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi")) + " " + year);
        model.addAttribute("monthOffset", monthOffset);

        logger.info("Statistics for {}-{}: weeklyStats={}, technicianWorkload={}", year, month, weeklyStats, technicianWorkload);
        return "admin/statistics";
    }
    @PostMapping("/appointments/change-technician")
    public String changeTechnician(@RequestParam Long appointmentId,
                                   @RequestParam Long technicianId,
                                   @RequestParam(defaultValue = "0") int weekOffset,
                                   RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            return "error/403";
        }

        try {
            appointmentService.changeTechnician(appointmentId, technicianId);
            redirectAttributes.addFlashAttribute("success", "Đổi kỹ thuật viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        redirectAttributes.addAttribute("weekOffset", weekOffset);
        return "redirect:/admin/appointments/schedule";
    }
    @PostMapping("/appointments/reschedule")
    public String rescheduleAppointment(
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam("newStartTime") String newStartTimeStr,
            @RequestParam(value = "weekOffset", defaultValue = "0") int weekOffset,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "technicianIdFilter", required = false) Long technicianIdFilter,
            RedirectAttributes redirectAttributes) {



        try {
            LocalDateTime newStartTime = LocalDateTime.parse(newStartTimeStr, DATETIME_FORMATTER);
            logger.info("Admin rescheduling appointment ID: {} to new start time: {}", appointmentId, newStartTime);
            appointmentService.rescheduleAppointment(appointmentId, newStartTime);
            redirectAttributes.addFlashAttribute("success", "Thay đổi thời gian lịch hẹn thành công!");
        } catch (IllegalArgumentException e) {
            logger.error("Error rescheduling appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error rescheduling appointment: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Đã có lỗi xảy ra. Vui lòng thử lại.");
        }

        // Giữ lại các tham số lọc khi redirect
        redirectAttributes.addAttribute("weekOffset", weekOffset);
        redirectAttributes.addAttribute("serviceName", serviceName);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("technicianId", technicianIdFilter);
        return "redirect:/admin/appointments/schedule";
    }
    @GetMapping("/dashboard")
    public String showAdminDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            return "error/403";
        }
        return "admin/adminDashboard";
    }
    @GetMapping("/ratings")
    public String listRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchTerm,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            return "error/403";
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Rating> ratingPage;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            ratingPage = ratingService.searchRatings(searchTerm, pageable);
        } else {
            ratingPage = ratingService.getAllRatings(pageable);
        }


        model.addAttribute("ratingPage", ratingPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("totalPages", ratingPage.getTotalPages());
        model.addAttribute("totalItems", ratingPage.getTotalElements());
        model.addAttribute("searchTerm", searchTerm);

        logger.info("Admin accessed ratings page. Current page: {}, Total ratings: {}, Search Term: {}", page, ratingPage.getTotalElements(), searchTerm);
        return "admin/rating-list";
    }

    @PostMapping("/ratings/delete/{id}")
    public String deleteRating(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(auth)) {
            logger.error("Access denied for user: {}", auth.getName());
            return "error/403";
        }
        try {
            ratingService.deleteRating(id);
            redirectAttributes.addFlashAttribute("success", "Đánh giá đã được xóa thành công!");
            logger.info("Rating with ID {} deleted by admin.", id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            logger.error("Error deleting rating {}: {}", id, e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống khi xóa đánh giá.");
            logger.error("Unexpected error deleting rating {}: {}", id, e.getMessage());
        }
        return "redirect:/admin/ratings";
    }
}
