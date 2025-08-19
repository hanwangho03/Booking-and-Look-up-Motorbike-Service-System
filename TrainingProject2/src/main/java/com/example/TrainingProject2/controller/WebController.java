package com.example.TrainingProject2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {
    @GetMapping("/")
    public String homePage() {
        return "carofix/index";
    }
    @GetMapping("/about")
    public String aboutUs() {
        return "carofix/about";
    }
    @GetMapping("/project")
    public String project() {
        return "carofix/project";
    }
    @GetMapping("/project-details")
    public String projectDetails() {
        return "carofix/project-details";
    }
    @GetMapping("/our-team")
    public String ourTeam() {
        return "carofix/team";
    }
    @GetMapping("/services")
    public String services() {
        return "carofix/service";
    }
    @GetMapping("/services-details")
    public String servicesDetails() {
        return "carofix/service-details";
    }
    @GetMapping("/chatbot")
    public String chatBot() {
        return "appointment/chatbot";
    }
}
