package com.example.Trainning.Project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse <T> {
    private boolean success;
    private String message;
    private T data;
    private ApiError error;
    private int statusCode;
    public ApiResponse(boolean success, String message, T data, int statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
    }
    public ApiResponse(boolean success, String message, ApiError error, int statusCode) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.statusCode = statusCode;
    }

}
