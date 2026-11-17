package com.dailyai.assistant.controller;

import com.dailyai.assistant.dto.PageResponse;
import com.dailyai.assistant.dto.ToolDto;
import com.dailyai.assistant.service.ToolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public PageResponse<ToolDto> list(@RequestParam(required = false) String category,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return toolService.list(category, q, page, size);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return toolService.categories();
    }

    @GetMapping("/{id}")
    public ToolDto detail(@PathVariable Long id) {
        return toolService.get(id);
    }
}
