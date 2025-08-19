package com.example.Trainning.Project.dto.repair;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairHistoryDTO {
    private Long id;
    private LocalDateTime sessionDate;
    private double totalCost;
    private String technicianNotes;
    private List<ServicePerformedDTO> servicesPerformed;
    private List<PartUsedDTO> partsUsed;
    private String technicianFullName;
    private Integer customerRating;
    private String customerComment;
    private String reviewerFullName;
    private Long customerId;

}