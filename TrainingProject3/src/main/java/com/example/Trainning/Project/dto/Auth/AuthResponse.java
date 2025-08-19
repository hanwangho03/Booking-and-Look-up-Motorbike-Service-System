package com.example.Trainning.Project.dto.Auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private final String jwt;
}