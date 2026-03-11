package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.dto.AttendanceSummaryDto;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public record EventCreateResponse(
        Long id,
        String title,
        LocalDateTime startTime,
        EventType type,
        Integer minAttendees,
        String description,
        Long groupId,
        GroupCreatorDto createdBy,
        LocalDateTime createdAt,
        AttendanceSummaryDto attendanceSummary
) {
    public static EventCreateResponse from(Event event, GroupCreatorDto createdBy, AttendanceSummaryDto attendanceSummary) {
        return new EventCreateResponse(
                event.getId(),
                event.getTitle(),
                event.getStartTime(),
                event.getType(),
                event.getMinAttendees(),
                event.getDescription(),
                event.getGroup().getId(),
                createdBy,
                event.getCreatedAt(),
                attendanceSummary
        );
    }
}
