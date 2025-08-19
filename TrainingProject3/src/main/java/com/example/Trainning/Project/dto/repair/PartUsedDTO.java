package com.example.Trainning.Project.dto.repair;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartUsedDTO {
    private String partName;
    private Integer quantity;
    private double unitPrice;
}