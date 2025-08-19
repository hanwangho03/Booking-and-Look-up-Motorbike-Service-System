package com.example.Trainning.Project.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI()); // ADD THIS
        return "admin/dashboard";
    }
    @GetMapping("/error/403")
    public String AccessDenied() {
        return "/error/403";
    }
    @GetMapping("/sessions")
    public String sessionDashboard() {
        return "admin_dashboard";
    }
    @GetMapping("/vehicles")
    public String getVehiclesAdminPage() {
        return "admin/vehicles-admin"; // Trả về tên của file HTML (không có .html)
    }
}