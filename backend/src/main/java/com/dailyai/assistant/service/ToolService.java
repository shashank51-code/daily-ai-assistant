package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.PageResponse;
import com.dailyai.assistant.dto.ToolDto;
import com.dailyai.assistant.entity.AIResource;
import com.dailyai.assistant.entity.Bookmark;
import com.dailyai.assistant.exception.ResourceNotFoundException;
import com.dailyai.assistant.repository.AIResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ToolService {

    private final AIResourceRepository resourceRepository;
    private final BookmarkService bookmarkService;

    public ToolService(AIResourceRepository resourceRepository, BookmarkService bookmarkService) {
        this.resourceRepository = resourceRepository;
        this.bookmarkService = bookmarkService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ToolDto> list(String category, String query, int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("name").ascending());
        Set<Long> bookmarked = bookmarkService.bookmarkedIds(Bookmark.ItemType.TOOL);

        Page<AIResource> result;
        if (query != null && !query.isBlank()) {
            result = resourceRepository.search(query.trim(), pageable);
        } else if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            result = resourceRepository.findByCategoryIgnoreCase(category.trim(), pageable);
        } else {
            result = resourceRepository.findAll(pageable);
        }
        return PageResponse.from(result, resource -> ToolDto.from(resource, bookmarked.contains(resource.getId())));
    }

    @Transactional(readOnly = true)
    public ToolDto get(Long id) {
        AIResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tool " + id + " was not found"));
        return ToolDto.from(resource, bookmarkService.isBookmarked(Bookmark.ItemType.TOOL, id));
    }

    @Transactional(readOnly = true)
    public List<ToolDto> trending(int limit) {
        Set<Long> bookmarked = bookmarkService.bookmarkedIds(Bookmark.ItemType.TOOL);
        return resourceRepository.findByTrendingTrueOrderByNameAsc().stream()
                .limit(Math.max(1, limit))
                .map(resource -> ToolDto.from(resource, bookmarked.contains(resource.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return resourceRepository.findAllByOrderByNameAsc().stream()
                .map(AIResource::getCategory)
                .distinct()
                .toList();
    }
}
