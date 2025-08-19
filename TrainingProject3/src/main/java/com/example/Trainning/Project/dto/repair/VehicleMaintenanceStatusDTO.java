// src/main/java/com/example/Trainning/Project/dto/maintenance/VehicleMaintenanceStatusDTO.java
package com.example.Trainning.Project.dto.repair;

import com.example.Trainning.Project.dto.vehicle.VehicleDisplayDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMaintenanceStatusDTO {
    private VehicleDisplayDTO vehicle;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMaintenanceDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextRecommendedMaintenanceDate;
    private int assumedMaintenanceIntervalMonths;
    private String status;
    private String notes;
}