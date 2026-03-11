package de.ceddyie.organizerbackend.dto.requests;

import de.ceddyie.organizerbackend.enums.EventType;

import java.time.LocalDateTime;

public record EventCreateRequest(
        String title,
        LocalDateTime startTime,
        EventType type,
        Integer minAttendees,
        String description
) {
}
