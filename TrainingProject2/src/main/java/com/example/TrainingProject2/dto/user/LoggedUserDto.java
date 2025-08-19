package com.example.TrainingProject2.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoggedUserDto {
    private String username;
    private String roleName;
    private String phoneNumber;
    private String name;
}
