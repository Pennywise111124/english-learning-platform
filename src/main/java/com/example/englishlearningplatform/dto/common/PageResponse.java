package com.example.englishlearningplatform.dto.common;

import org.springframework.data.domain.Page;
import java.util.List;

public class PageResponse<T> {

    private final List<T> content;
    private final int totalPages;
    private final long totalElements;
    private final int page;
    private final int size;

    public PageResponse(List<T> content, int totalPages, long totalElements, int page, int size) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
    }

    // Factory tiện dùng trong Service:
    // PageResponse.from(topicRepository.findAll(pageable).map(TopicResponse::from))
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(), // "number" bên Spring Data → map lại thành "page" cho khớp FE contract
                page.getSize());
    }

    public List<T> getContent() {
        return content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}