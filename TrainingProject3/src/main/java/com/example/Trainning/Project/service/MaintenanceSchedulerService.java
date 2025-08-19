package com.example.Trainning.Project.service;

import com.example.Trainning.Project.dto.repair.VehicleMaintenanceStatusDTO;
import com.example.Trainning.Project.dto.vehicle.VehicleDisplayDTO;
import com.example.Trainning.Project.model.Customer;
import com.example.Trainning.Project.model.ServiceSession;
import com.example.Trainning.Project.model.User;
import com.example.Trainning.Project.model.Vehicle;
import com.example.Trainning.Project.repository.ServiceSessionRepository;
import com.example.Trainning.Project.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceSchedulerService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceSessionRepository serviceSessionRepository;

    @Autowired
    private NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final int ASSUMED_MAINTENANCE_INTERVAL_MONTHS = 6;

    // 0 * * * * ? nhắc nhở mỗi phút
    @Scheduled(cron = "0 0 0 1 * ?") // Chạy vào ngày đầu tiên của mỗi tháng lúc 2:00 sáng
    @Transactional
    public void sendAutomatedMaintenanceReminders() {
        System.out.println("SCHEDULER: Bắt đầu chạy tác vụ gửi email nhắc nhở bảo trì tự động...");

        List<Vehicle> allVehicles = vehicleRepository.findAllVehiclesWithCustomerAndUser();

        if (allVehicles.isEmpty()) {
            System.out.println("SCHEDULER: Không tìm thấy xe nào trong hệ thống.");
            return;
        }

        int sentCount = 0;
        LocalDate currentDate = LocalDate.now();

        for (Vehicle vehicle : allVehicles) {
            // Tính toán trạng thái bảo trì cho từng xe
            VehicleMaintenanceStatusDTO statusDTO = calculateMaintenanceStatusForScheduledJob(vehicle);

            // Chỉ gửi email nếu xe quá hạn hoặc sắp đến hạn VÀ email chưa được gửi trong tháng này
            if (("Overdue".equals(statusDTO.getStatus()) || "Due Soon".equals(statusDTO.getStatus())) &&
                    (vehicle.getLastReminderSentDate() == null ||
                            vehicle.getLastReminderSentDate().getMonth() != currentDate.getMonth() ||
                            vehicle.getLastReminderSentDate().getYear() != currentDate.getYear())) {

                Customer customer = vehicle.getCustomer();
                if (customer != null && customer.getUser() != null) {
                    String toEmail = customer.getEmail();
                    String customerName = customer.getFullName();
                    String licensePlate = vehicle.getLicensePlate();

                    if (toEmail != null && !toEmail.isEmpty()) {
                        String status = statusDTO.getStatus();
                        String notes = statusDTO.getNotes();
                        String lastMaintenance = statusDTO.getLastMaintenanceDate() != null ? statusDTO.getLastMaintenanceDate().format(DATE_FORMATTER) : "N/A";
                        String nextMaintenance = statusDTO.getNextRecommendedMaintenanceDate() != null ? statusDTO.getNextRecommendedMaintenanceDate().format(DATE_FORMATTER) : "N/A";

                        System.out.println("SCHEDULER: Gửi email nhắc nhở cho xe " + licensePlate + " (" + status + ") tới " + toEmail);
                        notificationService.sendMaintenanceReminderEmail(
                                toEmail, customerName, licensePlate,
                                lastMaintenance, nextMaintenance,
                                status, notes
                        );
                        sentCount++;

                        // Cập nhật ngày gửi nhắc nhở cuối cùng cho xe này
                        vehicle.setLastReminderSentDate(currentDate);
                        vehicleRepository.save(vehicle);
                        System.out.println("SCHEDULER: Đã cập nhật lastReminderSentDate cho xe " + licensePlate + " thành " + currentDate);
                    } else {
                        System.out.println("SCHEDULER: Bỏ qua xe " + licensePlate + ": Khách hàng không có địa chỉ email.");
                    }
                } else {
                    System.out.println( "Không tìm thấy thông tin khách hàng hoặc người dùng.");
                }
            } else {
                System.out.println("SCHEDULER: Bỏ qua xe " + vehicle.getLicensePlate() + ": Không cần gửi nhắc nhở hoặc đã gửi trong tháng này.");
            }
        }
        System.out.println("SCHEDULER: Tác vụ gửi email nhắc nhở bảo trì tự động hoàn tất. Đã gửi " + sentCount + " email.");
    }


    /**
     * Logic tính toán trạng thái bảo trì.
     */
    private VehicleMaintenanceStatusDTO calculateMaintenanceStatusForScheduledJob(Vehicle vehicle) {
        Optional<ServiceSession> latestSessionOpt = serviceSessionRepository.findTopByVehicleOrderBySessionDateDesc(vehicle);

        LocalDate lastMaintenanceDate = null;
        if (latestSessionOpt.isPresent()) {
            lastMaintenanceDate = latestSessionOpt.get().getSessionDate().toLocalDate();
        }

        LocalDate nextRecommendedMaintenanceDate = null;
        if (lastMaintenanceDate != null) {
            nextRecommendedMaintenanceDate = lastMaintenanceDate.plusMonths(ASSUMED_MAINTENANCE_INTERVAL_MONTHS);
        }

        String status = "Unknown";
        String notes = "Dựa trên lịch sử sửa chữa và chu kỳ " + ASSUMED_MAINTENANCE_INTERVAL_MONTHS + " tháng mặc định.";
        LocalDate currentDate = LocalDate.now();

        if (lastMaintenanceDate == null) {
            status = "No History";
            notes = "Chưa có lịch sử sửa chữa. Không thể xác định chu kỳ bảo dưỡng.";
        } else if (nextRecommendedMaintenanceDate != null) {
            if (currentDate.isAfter(nextRecommendedMaintenanceDate)) {
                status = "Overdue";
                notes = "Đã quá hạn bảo dưỡng từ ngày " + nextRecommendedMaintenanceDate.format(DATE_FORMATTER) + ".";
            } else if (currentDate.isAfter(nextRecommendedMaintenanceDate.minusMonths(1))) {
                status = "Due Soon";
                notes = "Sắp đến hạn bảo dưỡng vào ngày " + nextRecommendedMaintenanceDate.format(DATE_FORMATTER) + ".";
            } else {
                status = "OK";
                notes = "Hiện tại không cần bảo dưỡng. Lần bảo dưỡng tiếp theo dự kiến: " + nextRecommendedMaintenanceDate.format(DATE_FORMATTER) + ".";
            }
        }

        VehicleDisplayDTO vehicleDTO = new VehicleDisplayDTO(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear()
        );

        return new VehicleMaintenanceStatusDTO(
                vehicleDTO,
                lastMaintenanceDate,
                nextRecommendedMaintenanceDate,
                ASSUMED_MAINTENANCE_INTERVAL_MONTHS,
                status,
                notes
        );
    }
}