package com.example.Trainning.Project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String message;
    private int statusCode;
    private List<String> details;
    public ApiError(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
