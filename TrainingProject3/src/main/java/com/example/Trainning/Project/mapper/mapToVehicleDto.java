package com.example.Trainning.Project.mapper;

import com.example.Trainning.Project.dto.vehicle.VehicleDto;
import com.example.Trainning.Project.model.Vehicle;

public class mapToVehicleDto {
    public static VehicleDto mapToVehicleDto(Vehicle vehicle){
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
