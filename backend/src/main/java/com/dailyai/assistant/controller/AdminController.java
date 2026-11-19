package com.dailyai.assistant.controller;

import com.dailyai.assistant.dto.ExplanationDto;
import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin operations are protected by Spring Security: an authenticated account
 * with ROLE_ADMIN, or a valid X-Admin-Key mapped to ROLE_ADMIN by the filter.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/news/refresh")
    public RefreshResultDto refresh() {
        return adminService.refreshNews();
    }

    @PostMapping("/articles/{id}/explain")
    public ExplanationDto explain(@PathVariable Long id) {
        return adminService.explain(id);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return adminService.status();
    }
}
