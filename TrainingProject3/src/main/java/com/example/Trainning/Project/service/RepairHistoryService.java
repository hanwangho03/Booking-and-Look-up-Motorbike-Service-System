package com.example.Trainning.Project.service;

import com.example.Trainning.Project.dto.repair.*;
import com.example.Trainning.Project.dto.vehicle.VehicleDisplayDTO;
import com.example.Trainning.Project.model.Customer;
import com.example.Trainning.Project.model.ServiceSession;
import com.example.Trainning.Project.model.User;
import com.example.Trainning.Project.model.Vehicle;
import com.example.Trainning.Project.repository.CustomerRepository;
import com.example.Trainning.Project.repository.ServiceSessionRepository;
import com.example.Trainning.Project.repository.VehicleRepository;
import com.lowagie.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RepairHistoryService {

    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ServiceSessionRepository serviceSessionRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private PdfService pdfService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Transactional(readOnly = true)
    public List<VehicleRepairHistoryResponse> searchRepairHistory(String query) {
        List<Vehicle> vehicles = new ArrayList<>();

        Optional<Vehicle> vehicleByLicensePlate = vehicleRepository.findByLicensePlate(query);
        vehicleByLicensePlate.ifPresent(vehicles::add);

        if (query.matches("^\\d{9,15}$")) {
            List<Vehicle> vehiclesByPhoneNumber = vehicleRepository.findByCustomer_User_PhoneNumber(query);
            vehiclesByPhoneNumber.forEach(v -> {
                if (!vehicles.contains(v)) {
                    vehicles.add(v);
                }
            });
        }

        if (vehicles.isEmpty()) {
            return Collections.emptyList();
        }

        return vehicles.stream()
                .map(this::convertToVehicleRepairHistoryResponse)
                .collect(Collectors.toList());
    }

    private VehicleRepairHistoryResponse convertToVehicleRepairHistoryResponse(Vehicle vehicle) {
        VehicleDisplayDTO vehicleDTO = new VehicleDisplayDTO(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear()
        );

        List<ServiceSession> serviceSessions = serviceSessionRepository.findByVehicleOrderBySessionDateDesc(vehicle);

        List<RepairHistoryDTO> repairHistoryDTOs = serviceSessions.stream()
                .map(this::convertToRepairHistoryDTO)
                .collect(Collectors.toList());

        return new VehicleRepairHistoryResponse(vehicleDTO, repairHistoryDTOs);
    }

    private RepairHistoryDTO convertToRepairHistoryDTO(ServiceSession serviceSession) {
        double serviceCost = serviceSession.getSessionServices() != null ?
                serviceSession.getSessionServices().stream()
                        .filter(ss -> ss.getCost() != null)
                        .mapToDouble(ss -> ss.getCost().doubleValue())
                        .sum() : 0.0;

        double partCost = serviceSession.getSessionParts() != null ?
                serviceSession.getSessionParts().stream()
                        .filter(sp -> sp.getUnitPrice() != null && sp.getQuantity() != null)
                        .mapToDouble(sp -> sp.getUnitPrice().doubleValue() * sp.getQuantity())
                        .sum() : 0.0;

        double totalCost = serviceCost + partCost;

        serviceSession.setTotalCost(BigDecimal.valueOf(totalCost));
        serviceSessionRepository.save(serviceSession);

        LocalDateTime sessionDate = serviceSession.getSessionDate() != null ?
                serviceSession.getSessionDate() : LocalDateTime.now();
        String technicianNotes = serviceSession.getTechnicianNotes() != null ?
                serviceSession.getTechnicianNotes() : "Không có ghi chú";

        List<ServicePerformedDTO> serviceDTOs = serviceSession.getSessionServices() != null ?
                serviceSession.getSessionServices().stream()
                        .map(ss -> new ServicePerformedDTO(
                                ss.getService() != null ? ss.getService().getName() : "Dịch vụ không xác định",
                                ss.getCost() != null ? ss.getCost().doubleValue() : 0.0
                        ))
                        .collect(Collectors.toList()) : new ArrayList<>();

        List<PartUsedDTO> partDTOs = serviceSession.getSessionParts() != null ?
                serviceSession.getSessionParts().stream()
                        .map(sp -> new PartUsedDTO(
                                sp.getPart() != null ? sp.getPart().getName() : "Phụ tùng không xác định",
                                sp.getQuantity() != null ? sp.getQuantity() : 0,
                                sp.getUnitPrice() != null ? sp.getUnitPrice().doubleValue() : 0.0
                        ))
                        .collect(Collectors.toList()) : new ArrayList<>();

        String technicianFullName = "Đang cập nhật";
        if (serviceSession.getTechnician() != null) {
            if (serviceSession.getTechnician().getFullName() != null &&
                    !serviceSession.getTechnician().getFullName().isEmpty()) {
                technicianFullName = serviceSession.getTechnician().getFullName();
            } else {
                technicianFullName = serviceSession.getTechnician().getEmployeeCode();
            }
        }
        Long customerId = null;
        if (serviceSession.getVehicle() != null && serviceSession.getVehicle().getCustomer() != null) {
            customerId = serviceSession.getVehicle().getCustomer().getId();
        }

        Integer customerRating = null;
        String customerComment = null;
        String reviewerFullName = null;
        if (serviceSession.getReview() != null) {
            customerRating = serviceSession.getReview().getRating();
            customerComment = serviceSession.getReview().getComment();
            if (serviceSession.getReview().getCustomer() != null) {
                reviewerFullName = serviceSession.getReview().getCustomer().getFullName();
            }
        }

        return new RepairHistoryDTO(
                serviceSession.getId(),
                sessionDate,
                totalCost,
                technicianNotes,
                serviceDTOs,
                partDTOs,
                technicianFullName,
                customerRating,
                customerComment,
                reviewerFullName,
                customerId
        );
    }

    @Transactional(readOnly = true)
    public List<VehicleMaintenanceStatusDTO> checkMaintenanceStatus(String query) {
        List<Vehicle> vehicles = new ArrayList<>();

        if (query.matches("^\\d{9,15}$")) {
            vehicles = vehicleRepository.findByCustomer_User_PhoneNumberWithCustomerAndUser(query);
        } else {
            Optional<Vehicle> vehicleByLicensePlate = vehicleRepository.findByLicensePlateWithCustomerAndUser(query);
            vehicleByLicensePlate.ifPresent(vehicles::add);
        }

        if (vehicles.isEmpty()) {
            return Collections.emptyList();
        }

        List<VehicleMaintenanceStatusDTO> results = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            VehicleMaintenanceStatusDTO statusDTO = calculateMaintenanceStatus(vehicle);
            results.add(statusDTO);
        }
        return results;
    }

    private VehicleMaintenanceStatusDTO calculateMaintenanceStatus(Vehicle vehicle) {
        Optional<ServiceSession> latestSessionOpt = serviceSessionRepository.findTopByVehicleOrderBySessionDateDesc(vehicle);

        LocalDate lastMaintenanceDate = null;
        if (latestSessionOpt.isPresent()) {
            lastMaintenanceDate = latestSessionOpt.get().getSessionDate().toLocalDate();
        }

        final int ASSUMED_MAINTENANCE_INTERVAL_MONTHS = 6;

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

    @Transactional(readOnly = true)
    public List<AdminRepairHistoryEntry> getAllRepairHistories() {
        List<ServiceSession> allServiceSessions = serviceSessionRepository.findAllByOrderBySessionDateDesc();

        return allServiceSessions.stream()
                .map(this::convertToAdminRepairHistoryEntry)
                .collect(Collectors.toList());
    }

    private AdminRepairHistoryEntry convertToAdminRepairHistoryEntry(ServiceSession serviceSession) {
        RepairHistoryDTO repairHistoryDTO = convertToRepairHistoryDTO(serviceSession);

        String licensePlate = "N/A";
        String customerFullName = "N/A";
        String customerPhoneNumber = "N/A";

        if (serviceSession.getVehicle() != null) {
            licensePlate = serviceSession.getVehicle().getLicensePlate();
            if (serviceSession.getVehicle().getCustomer() != null) {
                Customer customer = serviceSession.getVehicle().getCustomer();
                customerFullName = customer.getFullName();
                if (customer.getUser() != null) {
                    customerPhoneNumber = customer.getUser().getPhoneNumber();
                }
            }
        }

        return new AdminRepairHistoryEntry(
                repairHistoryDTO,
                licensePlate,
                customerFullName,
                customerPhoneNumber
        );
    }

    @Transactional(readOnly = true)
    public VehicleRepairHistoryResponse getRepairHistoryBySessionId(Long serviceSessionId) {
        Optional<ServiceSession> sessionOpt = serviceSessionRepository.findById(serviceSessionId);
        if (!sessionOpt.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy phiên sửa chữa với ID: " + serviceSessionId);
        }

        ServiceSession session = sessionOpt.get();
        Vehicle vehicle = session.getVehicle();
        if (vehicle == null) {
            throw new IllegalArgumentException("Phiên sửa chữa không liên kết với xe nào.");
        }

        VehicleDisplayDTO vehicleDTO = new VehicleDisplayDTO(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear()
        );

        List<RepairHistoryDTO> repairHistoryDTOs = Collections.singletonList(convertToRepairHistoryDTO(session));
        return new VehicleRepairHistoryResponse(vehicleDTO, repairHistoryDTOs);
    }

    @Transactional(readOnly = true)
    public byte[] generateRepairHistoryPdfByLicensePlate(String licensePlate) throws DocumentException, IOException {
        List<VehicleRepairHistoryResponse> repairHistories = searchRepairHistory(licensePlate);
        return pdfService.generateRepairHistoryPdf(repairHistories);
    }

    @Transactional(readOnly = true)
    public byte[] generateRepairHistoryPdfBySessionId(Long serviceSessionId) throws DocumentException, IOException {
        VehicleRepairHistoryResponse repairHistory = getRepairHistoryBySessionId(serviceSessionId);
        return pdfService.generateRepairHistoryPdf(Collections.singletonList(repairHistory));
    }
}