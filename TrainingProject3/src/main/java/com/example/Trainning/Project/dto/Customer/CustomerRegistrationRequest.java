package com.example.Trainning.Project.dto.Customer;

import com.example.Trainning.Project.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PasswordMatches(message = "Mật khẩu xác nhận không khớp")
public class CustomerRegistrationRequest {

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Size(min = 9, max = 15, message = "Số điện thoại phải có từ 9 đến 15 ký tự.")
    @Pattern(regexp = "^[0-9]+$", message = "Số điện thoại chỉ được chứa ký tự số.")
    private String phoneNumber;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Mật khẩu phải chứa ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@$!%*?&).")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống.")
    private String confirmPassword;

    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự.")
    private String fullName;

    @Email(message = "Địa chỉ email không hợp lệ.")
    @NotBlank(message = "Email không được để trống.")
    @Size(max = 100, message = "Địa chỉ email không được vượt quá 100 ký tự.")
    private String email;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String address;

}