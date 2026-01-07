package com.example.downtime.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/web/downtimes";
    }

    @GetMapping("/monitor")
    public String redirectToMonitor() {
        return "redirect:/web/monitor";
    }
}