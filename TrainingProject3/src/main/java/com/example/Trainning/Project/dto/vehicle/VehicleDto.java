package com.example.Trainning.Project.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class VehicleDto {
    private Long id;

    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @NotBlank(message = "License plate cannot be blank")
    @Pattern(regexp = "^[A-Z0-9.-]{5,20}$", message = "Invalid license plate format")
    private String licensePlate;

    private String brand;
    private String model;

    @Min(value = 1900, message = "Year must be at least 1900")
    private Integer year;
    private String vinNumber;
}