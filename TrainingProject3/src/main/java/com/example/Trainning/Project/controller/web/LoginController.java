package com.example.Trainning.Project.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/api/auth/login")
    public String login() {
        return "auth/login";
    }
    @GetMapping("/api/auth/register")
    public String register() {
        return "auth/register";
    }
}