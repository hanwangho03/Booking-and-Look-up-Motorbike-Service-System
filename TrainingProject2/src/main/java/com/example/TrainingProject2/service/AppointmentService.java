package com.example.TrainingProject2.service;

import com.example.TrainingProject2.model.Appointment;
import com.example.TrainingProject2.model.ServiceFix;
import com.example.TrainingProject2.model.User;
import com.example.TrainingProject2.repository.AppointmentRepository;
import com.example.TrainingProject2.repository.ServiceRepository;
import com.example.TrainingProject2.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private EmailService emailService;

    private static final int MAX_APPOINTMENTS_PER_DAY = 3;


    public Appointment createAppointment(String username, Integer serviceId, LocalDateTime startTime) {
        logger.info("Creating appointment for username: {}, serviceId: {}, startTime: {}", username, serviceId, startTime);

        if (!isValidAppointmentTime(startTime)) {
            logger.error("Invalid appointment time: {}", startTime);
            throw new IllegalArgumentException("Thời gian đặt lịch không hợp lệ.");
        }

        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại."));
        ServiceFix service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại."));

        logger.info("Customer: {}, Service: {}", customer.getUsername(), service.getName());

        int userAppointmentsToday = appointmentRepository.countAppointmentsByCustomerAndDay(customer.getId(),startTime.toLocalDate());
        if(userAppointmentsToday >= MAX_APPOINTMENTS_PER_DAY){
            logger.warn("Customer {} has reached the daily appointment limit ({}).", username, MAX_APPOINTMENTS_PER_DAY);
            throw new IllegalArgumentException("Bạn chỉ được đặt tối đa " + MAX_APPOINTMENTS_PER_DAY + " lịch hẹn trong một ngày.");
        }
        LocalDateTime endTime = startTime.plusMinutes(20);

        long appointmentCount = appointmentRepository.countActiveAppointmentsAtStartTime(startTime);
        if (appointmentCount >= 2) {
            logger.error("Time slot is full at: {}", startTime);
            throw new IllegalArgumentException("Mốc thời gian đã đầy. Vui lòng chọn thời gian khác.");
        }

        User technician = assignTechnician(startTime, endTime);
        if (technician == null) {
            logger.error("No available technician for time slot: {}", startTime);
            throw new IllegalArgumentException("Không có kỹ thuật viên rảnh trong mốc thời gian này.");
        }

        logger.info("Assigned technician: {}", technician.getUsername());

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setService(service);
        appointment.setTechnician(technician);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(Appointment.Status.cho_xac_nhan);

        return appointmentRepository.save(appointment);
    }
    /**
     * Reschedules an existing appointment to a new start time.
     * Throws an exception if the appointment cannot be rescheduled.
     *
     * @param appointmentId The ID of the appointment to reschedule.
     * @param newStartTime The new start time for the appointment.
     * @return The updated Appointment object.
     */
    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, LocalDateTime newStartTime) {
        logger.info("Rescheduling appointment ID: {} to new start time: {}", appointmentId, newStartTime);

        // 1. Tìm lịch hẹn hiện có
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));

        // 2. Kiểm tra trạng thái của lịch hẹn
        if (appointment.getStatus() == Appointment.Status.da_huy || appointment.getStatus() == Appointment.Status.da_hoan_thanh) {
            logger.error("Cannot reschedule appointment ID: {} as its status is {}", appointmentId, appointment.getStatus());
            throw new IllegalArgumentException("Không thể thay đổi lịch hẹn đã bị hủy hoặc đã hoàn thành.");
        }

        // 3. Xác thực thời gian mới
        if (!isValidAppointmentTime(newStartTime)) {
            logger.error("Invalid new start time: {}", newStartTime);
            throw new IllegalArgumentException("Thời gian đặt lịch mới không hợp lệ.");
        }

        LocalDateTime newEndTime = newStartTime.plusMinutes(20);

        // 4. Kiểm tra xem mốc thời gian mới có bị đầy hay không
        long appointmentCount = appointmentRepository.countActiveAppointmentsAtStartTime(newStartTime);
        if (appointmentCount >= 2) {
            logger.error("New time slot is full at: {}", newStartTime);
            throw new IllegalArgumentException("Mốc thời gian mới đã đầy. Vui lòng chọn thời gian khác.");
        }

        // 5. Gán lại kỹ thuật viên cho khung giờ mới
        User newTechnician = assignTechnician(newStartTime, newEndTime);
        if (newTechnician == null) {
            logger.error("No available technician for new time slot: {}", newStartTime);
            throw new IllegalArgumentException("Không có kỹ thuật viên rảnh trong mốc thời gian mới này.");
        }

        logger.info("Assigned new technician: {} for appointment ID: {}", newTechnician.getUsername(), appointmentId);

        // 6. Cập nhật thông tin lịch hẹn
        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newEndTime);
        appointment.setTechnician(newTechnician);
        appointment.setStatus(Appointment.Status.cho_xac_nhan);

        // 7. Lưu và trả về lịch hẹn đã cập nhật
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment ID: {} successfully rescheduled to new start time: {}", appointmentId, newStartTime);

        // Gửi email thông báo
        if (updatedAppointment.getCustomer().getEmail() != null && !updatedAppointment.getCustomer().getEmail().isEmpty()) {
            emailService.sendAppointmentStatusEmail(updatedAppointment, "Lịch hẹn của bạn đã được thay đổi!", "rescheduled", "Lịch hẹn của bạn đã được thay đổi. Vui lòng kiểm tra lại thông tin chi tiết.");
        } else {
            logger.warn("Customer {} (ID: {}) does not have an email. Cannot send reschedule notification.", updatedAppointment.getCustomer().getUsername(), updatedAppointment.getCustomer().getId());
        }

        return updatedAppointment;
    }
    private boolean isValidAppointmentTime(LocalDateTime startTime) {
        LocalDate date = startTime.toLocalDate();
        LocalTime time = startTime.toLocalTime();
        LocalDateTime now = LocalDateTime.now();

        if (startTime.isBefore(now)) {
            logger.warn("Invalid appointment time: {}", startTime);
            return false;
        }

        // Chủ nhật nghỉ
        if (date.getDayOfWeek().getValue() == 7) {
            return false;
        }

        if (startTime.getMinute() % 20 != 0) {
            return false;
        }

        // Thứ 2 đến Thứ 6: 08:00 - 20:00
        if (date.getDayOfWeek().getValue() >= 1 && date.getDayOfWeek().getValue() <= 5) {
            return time.isAfter(LocalTime.of(7, 59)) && time.isBefore(LocalTime.of(20, 1));
        }

        // Thứ 7: 08:00 - 12:00
        if (date.getDayOfWeek().getValue() == 6) {
            return time.isAfter(LocalTime.of(7, 59)) && time.isBefore(LocalTime.of(12, 1));
        }

        return false;
    }

    private User assignTechnician(LocalDateTime startTime, LocalDateTime endTime) {
        List<User> technicians = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().getId() == 2)
                .collect(Collectors.toList());

        if (technicians.isEmpty()) {
            logger.warn("No technicians found with role_id = 2");
            return null;
        }

        // Tính ngày của lịch hẹn
        LocalDate appointmentDate = startTime.toLocalDate();
        LocalDateTime dayStart = appointmentDate.atStartOfDay();
        LocalDateTime dayEnd = appointmentDate.atTime(23, 59, 59);

        User selectedTechnician = null;
        long minAppointments = Long.MAX_VALUE;

        for (User technician : technicians) {
            List<Appointment> conflictingAppointment = appointmentRepository.findConflictingAppointmentsForTechnician(technician, startTime, endTime);
            if (conflictingAppointment.isEmpty()) {
                long appointmentCount = appointmentRepository.countActiveAppointmentsByTechnicianAndStartTimeBetween(technician, dayStart, dayEnd);
                if (appointmentCount < minAppointments) {
                    minAppointments = appointmentCount;
                    selectedTechnician = technician;
                }
            }
        }

        if (selectedTechnician != null) {
            logger.info("Assigned technician: {} with {} appointments on {}", selectedTechnician.getUsername(), minAppointments, appointmentDate);
        } else {
            logger.warn("No technician available for time slot: {}", startTime);
        }

        return selectedTechnician;
    }

    public List<Appointment> findAllAppointments() {
        logger.info("Fetching all appointments");
        return appointmentRepository.findAll();
    }

    // Phê duyệt lịch hẹn
    public void approveAppointment(Long appointmentId) {
        logger.info("Approving appointment with ID: {}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));
        if (appointment.getStatus() != Appointment.Status.cho_xac_nhan) {
            logger.error("Appointment ID: {} is not in 'cho_xac_nhan' status", appointmentId);
            throw new IllegalArgumentException("Chỉ có thể phê duyệt lịch hẹn ở trạng thái 'Chờ xác nhận'.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (appointment.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Đã quá thời hạn phê duyệt lịch hẹn. ");
        }
        appointment.setStatus(Appointment.Status.da_xac_nhan);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        if (savedAppointment.getCustomer().getEmail() != null && !savedAppointment.getCustomer().getEmail().isEmpty()) {
            emailService.sendAppointmentStatusEmail(savedAppointment, "Lịch hẹn của bạn đã được phê duyệt!", "approved","");
        } else {
            logger.warn("Customer {} (ID: {}) does not have an email. Cannot send approval notification.", savedAppointment.getCustomer().getUsername(), savedAppointment.getCustomer().getId());
        }
    }

    public List<Appointment> findAppointmentsByDateRange(LocalDateTime start, LocalDateTime end) {
        logger.info("Fetching appointments from {} to {}", start, end);
        return appointmentRepository.findByStartTimeBetween(start, end);
    }

    // Hủy lịch hẹn
    public void cancelAppointment(Long appointmentId, String reason) {
        logger.info("Cancelling appointment with ID: {} with reason: {}", appointmentId, reason);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));

        if (appointment.getStatus() == Appointment.Status.da_huy) {
            logger.warn("Appointment ID: {} has already been cancelled", appointmentId);
            throw new IllegalArgumentException("Lịch hẹn đã bị hủy.");
        }

        appointment.setStatus(Appointment.Status.da_huy);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        if (savedAppointment.getCustomer().getEmail() != null && !savedAppointment.getCustomer().getEmail().isEmpty()) {
            emailService.sendAppointmentStatusEmail(savedAppointment, "Lịch hẹn của bạn đã bị hủy!", "cancelled", reason); // TRUYỀN THÊM LÝ DO
        } else {
            logger.warn("Customer {} (ID: {}) does not have an email. Cannot send cancellation notification.", savedAppointment.getCustomer().getUsername(), savedAppointment.getCustomer().getId());
        }
    }

    public void completeAppointment(Long appointmentId) {
        logger.info("Completing appointment with ID: {}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại."));

        if (appointment.getStatus() != Appointment.Status.da_xac_nhan) {
            logger.error("Appointment ID: {} is not in 'da_xac_nhan' status", appointmentId);
            throw new IllegalArgumentException("Chỉ có thể hoàn thành lịch hẹn đã được xác nhận.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = appointment.getEndTime().minusHours(4);

        Duration timePassed = Duration.between(endTime, now);
        if (now.isBefore(endTime)) {
            logger.error("Appointment ID: {} chưa kết thúc", appointmentId);
            throw new IllegalArgumentException("Chưa thể hoàn thành lịch hẹn khi chưa đến thời điểm kết thúc.");
        }
        if (timePassed.toHours() > 8) {
            logger.error("Appointment ID: {} đã quá thời hạn 8h kể từ khi kết thúc", appointmentId);
            throw new IllegalArgumentException("Chỉ có thể hoàn thành lịch hẹn trong vòng 8 giờ sau khi kết thúc.");
        }

        appointment.setStatus(Appointment.Status.da_hoan_thanh);
        appointmentRepository.save(appointment);
    }

    public Map<String, Long> getWeeklyAppointmentStatistics(int year, int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate actualStartOfWeekForMonth = startOfMonth.with(weekFields.dayOfWeek(), 1);
        LocalDate actualEndOfWeekForMonth = endOfMonth.with(weekFields.dayOfWeek(), 7);

        LocalDateTime queryStartDate = actualStartOfWeekForMonth.atStartOfDay();
        LocalDateTime queryEndDate = actualEndOfWeekForMonth.plusDays(1).atStartOfDay().minusNanos(1);

        List<Object[]> results = appointmentRepository.countAppointmentsPerWeek(queryStartDate, queryEndDate);

        Map<String, Long> weeklyStats = new LinkedHashMap<>();
        Map<String, String> weekLabelsByYearWeekKey = new LinkedHashMap<>();

        LocalDate current = actualStartOfWeekForMonth;
        int weekCounter = 1;
        while (!current.isAfter(actualEndOfWeekForMonth)) {
            LocalDate startOfWeek = current.with(weekFields.dayOfWeek(), 1);
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            int weekOfYear = current.get(weekFields.weekOfWeekBasedYear());
            int weekBasedYear = current.get(weekFields.weekBasedYear());
            String yearWeekKey = weekBasedYear + "-" + weekOfYear;

            String label = "Tuần " + weekCounter + " (" +
                    startOfWeek.format(DateTimeFormatter.ofPattern("dd/MM")) + " - " +
                    endOfWeek.format(DateTimeFormatter.ofPattern("dd/MM")) + ")";

            weekLabelsByYearWeekKey.put(yearWeekKey, label);
            weeklyStats.put(label, 0L);

            current = current.plusWeeks(1);
            weekCounter++;
        }

        for (Object[] result : results) {
            String resultYearWeekKey = result[0] + "-" + result[1];
            Long count = (Long) result[2];

            String label = weekLabelsByYearWeekKey.get(resultYearWeekKey);
            if (label != null) {
                weeklyStats.put(label, count);
            }
        }

        logger.info("Weekly appointment stats for {}-{}: {}", year, month, weeklyStats);
        return weeklyStats;
    }

    public Map<String, Long> getTechnicianWorkloadStatistics(int year, int month) {
        List<Object[]> results = appointmentRepository.countTechnicianWorkloadPerMonth(year, month);
        Map<String, Long> workloadStats = results.stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Long) obj[1],
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));

        logger.info("Technician workload stats for {}-{}: {}", year, month, workloadStats);
        return workloadStats;
    }

    public void changeTechnician(Long appointmentId, Long technicianId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại"));

        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Kỹ thuật viên không tồn tại"));

        List<Appointment> conflict = appointmentRepository.findConflictingAppointmentsForTechnician(technician, appointment.getStartTime(), appointment.getEndTime());
        if (!conflict.isEmpty()) {
            throw new IllegalArgumentException("Kỹ thuật viên đã có lịch vào khung giờ này.");
        }

        appointment.setTechnician(technician);
        appointmentRepository.save(appointment);
    }

    public Page<Appointment> getAppointmentHistoryForCustomer(String username, int page, int size) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        return appointmentRepository.findByCustomerOrderByStartTimeDesc(customer, pageable);
    }

    public List<Appointment> findAppointmentsForTechnicianByDateRange(User technician, LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByTechnicianAndStartTimeBetween(technician, start, end);
    }

    public Optional<Appointment> getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId);
    }

    public List<ServiceFix> getAllServices() {
        return serviceRepository.findAll();
    }

    public List<User> getAllTechnicians() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().getId() == 2)
                .collect(Collectors.toList());
    }

    public List<Appointment.Status> getAllAppointmentStatuses() {
        return Arrays.asList(Appointment.Status.values());
    }

    public List<Appointment> findAppointmentsByDateRangeAndFilters(
            LocalDateTime start, LocalDateTime end,
            String serviceName, Appointment.Status status, Long technicianId) {

        logger.info("Fetching appointments with filters: serviceName={}, status={}, technicianId={} for range {} to {}",
                serviceName, status, technicianId, start, end);

        return appointmentRepository.findAppointmentsWithFilters(
                start, end, serviceName, status, technicianId
        );
    }

    @Scheduled(fixedRate = 36000000)
    @Transactional
    public void autoCancelPendingAppointment() {
        logger.info("Running scheduled task: Checking for pending appointments to auto-cancel...");

        LocalDateTime currentTime = LocalDateTime.now();

        List<Appointment> pendingAppointments = appointmentRepository.findPendingAppointmentsToAutoCancelByStartTime(Appointment.Status.cho_xac_nhan, currentTime);

        if (pendingAppointments.isEmpty()) {
            logger.info("No pending appointments found.");
            return;
        }

        for (Appointment appointment : pendingAppointments) {
            try {
                if (appointment.getStartTime().isBefore(LocalDateTime.now())) {
                    logger.warn("Appointment ID {} is pending but its start time {} has already passed. Skipping auto-cancellation for now.",
                            appointment.getId(), appointment.getStartTime());
                }
                appointment.setStatus(Appointment.Status.da_huy);
                Appointment savedAppointment = appointmentRepository.save(appointment);
                if (savedAppointment.getCustomer().getEmail() != null && !savedAppointment.getCustomer().getEmail().isEmpty()) {
                    emailService.sendAppointmentStatusEmail(savedAppointment, "Lịch hẹn của bạn đã bị hủy!", "cancelled","Lịch hẹn của bạn đã quá hạn vui lòng liên hệ lại với chúng tôi!!!");
                } else {
                    logger.warn("Customer {} (ID: {}) does not have an email. Cannot send cancellation notification.", savedAppointment.getCustomer().getUsername(), savedAppointment.getCustomer().getId());
                }
                logger.info("Auto-cancelled pending appointment ID {}.", appointment.getId());

            } catch (Exception e) {
                logger.error("Error auto-cancelling appointment ID {}: {}", appointment.getId(), e.getMessage(), e);
            }
        }
        logger.info("Finished auto-cancellation task. Total appointments cancelled: {}", pendingAppointments.size());
    }
}