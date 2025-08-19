package com.example.Trainning.Project.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Year;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDisplayDTO {
    private Long id;
    private String licensePlate;
    private String brand;
    private String model;
    private Year year;
}