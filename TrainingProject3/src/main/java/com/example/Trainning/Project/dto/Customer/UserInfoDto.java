package com.example.Trainning.Project.dto.Customer;

import lombok.Data;

@Data
public class UserInfoDto {
    private String username;
    private String phoneNumber;
    private String fullName;
    private String role;
    private Long customerId;
}