package com.managify.projects.controller;

import com.managify.exception.AppException;

final class Paging {

    static final int MAX_PAGE_SIZE = 100;

    private Paging() {}

    /** Validates a 1-based page number and clamps the size to 1..{@value #MAX_PAGE_SIZE}. */
    static int size(int page, Integer size, int defaultSize) {
        if (page < 1) {
            throw AppException.badRequest("page starts at 1");
        }
        int resolved = size == null ? defaultSize : size;
        return Math.max(1, Math.min(resolved, MAX_PAGE_SIZE));
    }
}
