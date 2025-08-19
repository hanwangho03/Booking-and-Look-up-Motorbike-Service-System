package com.example.Trainning.Project.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RepairHistoryWebController {

    @GetMapping("/repair-history")
    public String showRepairHistoryPage() {
        return "services/repair_history_lookup";
    }
}