package com.dailyai.assistant.controller;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.dto.ExplanationDto;
import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.exception.UnauthorizedException;
import com.dailyai.assistant.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AppProperties properties;

    public AdminController(AdminService adminService, AppProperties properties) {
        this.adminService = adminService;
        this.properties = properties;
    }

    @PostMapping("/news/refresh")
    public RefreshResultDto refresh(@RequestHeader(name = "X-Admin-Key", required = false) String adminKey) {
        requireAdminKey(adminKey);
        return adminService.refreshNews();
    }

    @PostMapping("/articles/{id}/explain")
    public ExplanationDto explain(@PathVariable Long id,
                                  @RequestHeader(name = "X-Admin-Key", required = false) String adminKey) {
        requireAdminKey(adminKey);
        return adminService.explain(id);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return adminService.status();
    }

    /**
     * The admin key is optional for local development. When it is configured the
     * header must match, so a deployed instance cannot be refreshed by strangers.
     */
    private void requireAdminKey(String provided) {
        String expected = properties.getAdminApiKey();
        if (expected == null || expected.isBlank()) {
            return;
        }
        if (provided == null || !expected.equals(provided)) {
            throw new UnauthorizedException("A valid X-Admin-Key header is required for this endpoint");
        }
    }
}
