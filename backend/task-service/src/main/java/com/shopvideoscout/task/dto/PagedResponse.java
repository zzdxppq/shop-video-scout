package com.shopvideoscout.task.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Story 5.5: 历史任务管理
 */
@Data
@Builder
public class PagedResponse<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;

    /**
     * Create a paged response.
     *
     * @param items items for current page
     * @param total total count of all items
     * @param page current page number (1-indexed)
     * @param size page size
     */
    public static <T> PagedResponse<T> of(List<T> items, long total, int page, int size) {
        boolean hasMore = (long) page * size < total;
        return PagedResponse.<T>builder()
            .items(items)
            .total(total)
            .page(page)
            .size(size)
            .hasMore(hasMore)
            .build();
    }
}
