package com.example.Trainning.Project.dto.vehicle;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAdminViewDto {
    // Thông tin xe
    private Long vehicleId;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer year;
    private String vinNumber;

    // Thông tin khách hàng
    private Long customerId;
    private String customerFullName;
    private String customerEmail;
    private String customerPhoneNumber;
    private LocalDateTime lastRepairDate;
}