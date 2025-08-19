package com.example.Trainning.Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("message", "Chào mừng bạn đến với Spring Boot! Setup đã thành công!");
        return "index";
    }
}