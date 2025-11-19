package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Paginated response wrapper
 * Sử dụng khi cần trả về dữ liệu phân trang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {

    private List<T> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean hasPrevious;

    private boolean hasNext;

    private boolean first;

    private boolean last;

    public static <T> PaginatedResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasPrevious(page.hasPrevious())
                .hasNext(page.hasNext())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
