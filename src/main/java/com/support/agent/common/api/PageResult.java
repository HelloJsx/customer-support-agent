package com.support.agent.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Lightweight pagination wrapper that does not leak Spring Data types to clients.
 */
public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
