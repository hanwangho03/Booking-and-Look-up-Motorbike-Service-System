
package com.example.Trainning.Project.controller.api;

import com.example.Trainning.Project.dto.Auth.AuthRequest;
import com.example.Trainning.Project.dto.Auth.AuthResponse;
import com.example.Trainning.Project.dto.Customer.CustomerRegistrationRequest;
import com.example.Trainning.Project.dto.response.ApiError;
import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> createAuthenticationToken(@RequestBody AuthRequest authRequest) {
        try {
            AuthResponse authResponseData = authService.authenticateUser(authRequest);
            ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(true, "Đăng nhập thành công!", authResponseData, HttpStatus.OK.value());
            return ResponseEntity.ok(apiResponse);
        } catch (BadCredentialsException e) {
            ApiError error = new ApiError("Số điện thoại hoặc mật khẩu không chính xác.", HttpStatus.UNAUTHORIZED.value());
            ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(false, "Đăng nhập thất bại.", error, HttpStatus.UNAUTHORIZED.value());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
        } catch (Exception e) {
            System.err.println("Lỗi trong quá trình đăng nhập: " + e.getMessage());
            ApiError error = new ApiError("Đã xảy ra lỗi không mong muốn trong quá trình đăng nhập. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR.value());
            ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(false, "Đăng nhập thất bại.", error, HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody CustomerRegistrationRequest registerRequest) {
        String successMessage = authService.registerNewCustomer(registerRequest);
        ApiResponse<Void> apiResponse = new ApiResponse<>(true, successMessage, null, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
}