// src/main/java/com/example/Trainning/Project/controller/api/UserController.java
package com.example.Trainning.Project.controller.api;

import com.example.Trainning.Project.dto.Customer.UserInfoDto;
import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserInfoDto>> getUserInfo() {
        Optional<UserInfoDto> userInfoOptional = userService.getCurrentUserInformation();

        if (userInfoOptional.isPresent()) {
            ApiResponse<UserInfoDto> apiResponse = new ApiResponse<>(true, "Lấy thông tin người dùng thành công!", userInfoOptional.get(), HttpStatus.OK.value());
            return ResponseEntity.ok(apiResponse);
        } else {
            ApiResponse<UserInfoDto> apiResponse = new ApiResponse<>(false, "Không tìm thấy thông tin người dùng.", null, HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }
}