package com.example.Trainning.Project.controller.api;

import com.example.Trainning.Project.dto.vehicle.VehicleAdminViewDto;
import com.example.Trainning.Project.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RestController này chỉ chịu trách nhiệm cung cấp các API liên quan đến quản lý xe.
 * Dữ liệu trả về sẽ luôn ở định dạng JSON.
 */
@RestController
@RequestMapping("/api/admin/vehicles")
public class VehicleAdminApiController {

    private final VehicleService vehicleService;

    public VehicleAdminApiController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * API endpoint để lấy danh sách tất cả xe và thông tin chủ sở hữu cho trang admin.
     * @return ResponseEntity chứa danh sách xe hoặc lỗi nếu có.
     */
    @GetMapping("/all")
    public ResponseEntity<List<VehicleAdminViewDto>> getAllVehiclesForAdmin() {
        try {
            List<VehicleAdminViewDto> vehicles = vehicleService.getAllVehiclesForAdmin();
            if (vehicles.isEmpty()) {
                // Trả về 204 No Content nếu không có dữ liệu
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            // Trả về 200 OK cùng với dữ liệu
            return new ResponseEntity<>(vehicles, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy dữ liệu xe cho admin: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}