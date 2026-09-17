package com.supplybase.partners.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Deliberately not returning Spring Data's {@link Page} directly from any
 * controller: its default JSON shape has changed across Spring Data
 * versions (flat legacy fields vs. a nested PagedModel-style "page" object)
 * and isn't a contract worth depending on implicitly. This is a small, fixed
 * shape every admin/paged endpoint returns instead.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
