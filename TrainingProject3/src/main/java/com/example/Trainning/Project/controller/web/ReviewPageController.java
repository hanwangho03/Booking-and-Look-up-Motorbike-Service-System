package com.example.Trainning.Project.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReviewPageController {
    @GetMapping("/review-form")
    public String reviewForm() {
        return "review/review-form";
    }
}
