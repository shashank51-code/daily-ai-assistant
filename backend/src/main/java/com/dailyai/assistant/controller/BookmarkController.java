package com.dailyai.assistant.controller;

import com.dailyai.assistant.dto.BookmarkDto;
import com.dailyai.assistant.dto.BookmarkRequest;
import com.dailyai.assistant.entity.Bookmark;
import com.dailyai.assistant.service.BookmarkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public List<BookmarkDto> list(@RequestParam(required = false) Bookmark.ItemType itemType) {
        return bookmarkService.list(itemType);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookmarkDto add(@Valid @RequestBody BookmarkRequest request) {
        return bookmarkService.add(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookmarkService.delete(id);
    }

    /** Removes a bookmark by the saved item itself, which is what the UI toggle needs. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByItem(@RequestParam Bookmark.ItemType itemType, @RequestParam Long itemId) {
        bookmarkService.deleteByItem(itemType, itemId);
    }
}
