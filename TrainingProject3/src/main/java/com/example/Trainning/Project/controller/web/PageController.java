package com.example.Trainning.Project.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/user-info")
    public String userInfo() {
        return "user/user-info";
    }
}
