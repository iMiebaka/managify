package com.managify.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/** A page of results. {@code pageNumber} is 1-based. */
@Getter
@Builder
public class PagedResponse<T> {

    private List<T> content;

    private int pageSize;

    private int totalPages;

    private long totalElements;

    private int pageNumber;

    private int currentElements;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageNumber(page.getPageable().getPageNumber() + 1)
                .currentElements(page.getNumberOfElements())
                .build();
    }
}
