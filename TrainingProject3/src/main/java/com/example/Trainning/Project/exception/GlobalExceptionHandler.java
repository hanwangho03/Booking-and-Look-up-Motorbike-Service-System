package com.example.Trainning.Project.exception;

import com.example.Trainning.Project.dto.response.ApiError;
import com.example.Trainning.Project.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiError apiError = new ApiError("Lỗi validation", HttpStatus.BAD_REQUEST.value(), errors);
        ApiResponse<Void> apiResponse = new ApiResponse<>(false, "Validation failed", apiError, HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        ApiError apiError = new ApiError(ex.getMessage(), HttpStatus.CONFLICT.value());
        ApiResponse<Void> apiResponse = new ApiResponse<>(false, "Đăng ký thất bại", apiError, HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PhoneNumberAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiResponse<Void>> handlePhoneNumberAlreadyExistsException(PhoneNumberAlreadyExistsException ex) {
        ApiError apiError = new ApiError(ex.getMessage(), HttpStatus.CONFLICT.value());
        ApiResponse<Void> apiResponse = new ApiResponse<>(false, "Đăng ký thất bại", apiError, HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        System.err.println("Lỗi máy chủ không mong muốn: " + ex.getMessage());
        ex.printStackTrace();

        ApiError apiError = new ApiError("Đã xảy ra lỗi không mong muốn trên máy chủ.", HttpStatus.INTERNAL_SERVER_ERROR.value());
        ApiResponse<Void> apiResponse = new ApiResponse<>(false, "Đăng ký thất bại", apiError, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}