package com.example.Trainning.Project.controller.api;

import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.dto.vehicle.VehicleDto;
import com.example.Trainning.Project.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;
    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }
    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleDto>>> getAllVehicles() {
        ApiResponse<List<VehicleDto>> response = vehicleService.getAllVehicles();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
