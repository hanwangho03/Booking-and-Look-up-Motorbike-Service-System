package com.example.Trainning.Project.service;

import com.example.Trainning.Project.dto.response.ApiError;
import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.dto.vehicle.VehicleAdminViewDto;
import com.example.Trainning.Project.dto.vehicle.VehicleDto;
import com.example.Trainning.Project.model.Customer;
import com.example.Trainning.Project.model.ServiceSession;
import com.example.Trainning.Project.model.User;
import com.example.Trainning.Project.model.Vehicle;
import com.example.Trainning.Project.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;


    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<VehicleDto>> getAllVehicles() {
        try {
            List<Vehicle> vehicles = vehicleRepository.findAll();

            if (vehicles.isEmpty()) {
                System.out.println("No vehicles found in the database.");
                ApiError apiError = new ApiError("NO_CONTENT", 404);
                return new ApiResponse<>(
                        false,
                        "Không tìm thấy xe nào trong hệ thống.",
                        apiError,
                        HttpStatus.NOT_FOUND.value()
                );
            }

            List<VehicleDto> vehicleDtos = vehicles.stream()
                    .map(this::mapToVehicleDto)
                    .collect(Collectors.toList());

            return new ApiResponse<>(
                    true,
                    "Lấy danh sách xe thành công.",
                    vehicleDtos,
                    HttpStatus.OK.value()
            );

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy tất cả xe: " + e.getMessage());
            ApiError apiError = new ApiError("INTERNAL_SERVER_ERROR", 500);
            return new ApiResponse<>(
                    false,
                    "Có lỗi xảy ra khi lấy danh sách xe, vui lòng thử lại sau.",
                    apiError,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }
    @Transactional(readOnly = true)
    public List<VehicleAdminViewDto> getAllVehiclesForAdmin() {
        List<Vehicle> vehicles = vehicleRepository.findAllVehiclesWithCustomerAndUser();
        return vehicles.stream()
                .map(this::mapToVehicleAdminViewDto)
                .collect(Collectors.toList());
    }

    private VehicleAdminViewDto mapToVehicleAdminViewDto(Vehicle vehicle) {
        VehicleAdminViewDto dto = new VehicleAdminViewDto();

        // Map thông tin xe
        dto.setVehicleId(vehicle.getId());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setBrand(vehicle.getBrand());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear() != null ? vehicle.getYear().getValue() : null);
        dto.setVinNumber(vehicle.getVinNumber());

        // Map thông tin khách hàng (kiểm tra null để an toàn)
        Customer customer = vehicle.getCustomer();
        if (customer != null) {
            dto.setCustomerId(customer.getId());
            dto.setCustomerFullName(customer.getFullName());
            dto.setCustomerEmail(customer.getEmail());

            User user = customer.getUser();
            if (user != null) {
                dto.setCustomerPhoneNumber(user.getPhoneNumber());
            }
        }

        // Lấy ngày sửa chữa gần nhất
        Optional<ServiceSession> latestSessionOptional = vehicleRepository.findTopByVehicleOrderBySessionDateDesc(vehicle);
        latestSessionOptional.ifPresent(session -> dto.setLastRepairDate(session.getSessionDate()));

        return dto;
    }

    private VehicleDto mapToVehicleDto(Vehicle vehicle) {
        VehicleDto dto = new VehicleDto();
        dto.setId(vehicle.getId());
        dto.setCustomerId(vehicle.getCustomer() != null ? vehicle.getCustomer().getId() : null);
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setBrand(vehicle.getBrand());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear() != null ? vehicle.getYear().getValue() : null);
        dto.setVinNumber(vehicle.getVinNumber());
        return dto;
    }
}