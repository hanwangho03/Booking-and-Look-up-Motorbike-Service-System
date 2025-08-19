package com.example.Trainning.Project.dto.Auth;

import lombok.Data;

@Data
public class AuthRequest {
    private String phoneNumber;
    private String password;
}