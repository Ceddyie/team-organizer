package de.ceddyie.organizerbackend.dto;

public record AttendanceSummaryDto(
        Integer accepted,
        Integer declined,
        Integer maybe,
        Integer noResponse
) {
}
