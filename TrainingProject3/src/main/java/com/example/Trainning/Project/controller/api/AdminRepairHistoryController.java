package com.example.Trainning.Project.controller.api;

import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.dto.repair.AdminRepairHistoryEntry;
import com.example.Trainning.Project.dto.vehicle.VehicleAdminViewDto;
import com.example.Trainning.Project.service.RepairHistoryService;
import com.example.Trainning.Project.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminRepairHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(AdminRepairHistoryController.class);

    @Autowired
    private RepairHistoryService repairHistoryService;
    private final VehicleService vehicleService;

    public AdminRepairHistoryController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/repair-history/all")
    public ResponseEntity<ApiResponse<List<AdminRepairHistoryEntry>>> getAllRepairHistoriesForAdmin() {
        logger.info("Admin requested to get all repair histories.");
        try {
            List<AdminRepairHistoryEntry> histories = repairHistoryService.getAllRepairHistories();
            if (histories.isEmpty()) {
                logger.info("No repair histories found for admin view.");
                return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(true, "Không tìm thấy lịch sử sửa chữa nào.", histories, HttpStatus.OK.value()));
            }
            logger.info("Retrieved {} repair histories for admin view.", histories.size());
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(true, "Lấy tất cả lịch sử sửa chữa thành công.", histories, HttpStatus.OK.value()));
        } catch (Exception e) {
            logger.error("Error retrieving all repair histories for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, "Lỗi khi lấy lịch sử sửa chữa: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    @GetMapping("/vehicles")
    @ResponseBody
    public List<VehicleAdminViewDto> getVehiclesAdminData() {
        return vehicleService.getAllVehiclesForAdmin();
    }
}