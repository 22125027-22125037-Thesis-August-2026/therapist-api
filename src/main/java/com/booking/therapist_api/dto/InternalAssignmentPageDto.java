package com.booking.therapist_api.dto;

import java.util.List;

/**
 * Stable, version-independent envelope for the reconciliation snapshot. We avoid
 * serializing Spring's {@code Page} directly because its JSON shape is not a
 * stable contract.
 */
public record InternalAssignmentPageDto(
        List<InternalAssignmentDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
