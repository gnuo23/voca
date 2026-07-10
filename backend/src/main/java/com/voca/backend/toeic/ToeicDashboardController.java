package com.voca.backend.toeic;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/toeic")
public class ToeicDashboardController {

    private final ToeicDashboardService dashboardService;

    public ToeicDashboardController(ToeicDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ToeicDashboardResponse dashboard(Authentication authentication) {
        return dashboardService.getDashboard(authentication);
    }
}
