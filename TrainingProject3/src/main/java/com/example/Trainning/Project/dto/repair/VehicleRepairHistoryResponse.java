package com.example.Trainning.Project.dto.repair;

import com.example.Trainning.Project.dto.vehicle.VehicleDisplayDTO;
import com.example.Trainning.Project.dto.vehicle.VehicleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRepairHistoryResponse {
    private VehicleDisplayDTO vehicle;
    private List<RepairHistoryDTO> repairHistories;
}