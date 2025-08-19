package com.example.TrainingProject2.dto.auth;

import com.example.TrainingProject2.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PasswordMatches
public class RegisterRequestDto {

    @NotEmpty(message = "Tên đăng nhập không được để trống.")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự.")
    private String username;

    @NotEmpty(message = "Mật khẩu không được để trống.")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự.")
    private String password;

    @NotEmpty(message = "Xác nhận mật khẩu không được để trống.")
    private String confirmPassword;

    @NotEmpty(message = "Họ và tên không được để trống.")
    private String name;

    @NotEmpty(message = "Số điện thoại không được để trống.")
    @Size(min = 10, max = 11, message = "Số điện thoại không hợp lệ.")
    private String phoneNumber;

    @NotEmpty(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ.")
    private String email;
}