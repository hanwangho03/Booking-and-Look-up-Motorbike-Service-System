package com.example.Trainning.Project.dto.repair;

public class AdminRepairHistoryEntry {
    private RepairHistoryDTO repairHistory;
    private String licensePlate;
    private String customerFullName;
    private String customerPhoneNumber;

    // Constructors
    public AdminRepairHistoryEntry(RepairHistoryDTO repairHistory, String licensePlate, String customerFullName, String customerPhoneNumber) {
        this.repairHistory = repairHistory;
        this.licensePlate = licensePlate;
        this.customerFullName = customerFullName;
        this.customerPhoneNumber = customerPhoneNumber;
    }

    public RepairHistoryDTO getRepairHistory() {
        return repairHistory;
    }

    public void setRepairHistory(RepairHistoryDTO repairHistory) {
        this.repairHistory = repairHistory;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public void setCustomerFullName(String customerFullName) {
        this.customerFullName = customerFullName;
    }

    public String getCustomerPhoneNumber() {
        return customerPhoneNumber;
    }

    public void setCustomerPhoneNumber(String customerPhoneNumber) {
        this.customerPhoneNumber = customerPhoneNumber;
    }
}