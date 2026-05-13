package com.dishari.in.web.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PaginatedResponse <T>(
        List<T> content ,
        int number ,
        int size ,
        long totalElements ,
        int totalPages ,
        boolean first ,
        boolean last ,
        boolean hasNext ,
        boolean hasPrevious
) {
    // Factory method — build from Spring Page
    public static <T, E> PaginatedResponse<T> of(
            Page<E> page, Function<E, T> mapper) {
        return new PaginatedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    public static <T> PaginatedResponse <T> fromEntity(List<T> content , Page<T> page) {
        return new PaginatedResponse<>(
                content ,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
