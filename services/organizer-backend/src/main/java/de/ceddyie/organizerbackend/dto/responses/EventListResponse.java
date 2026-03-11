package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.dto.AttendanceSummaryDto;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.model.Event;

import java.time.LocalDateTime;

public record EventListResponse(
        Long id,
        String title,
        LocalDateTime startTime,
        EventType type,
        Integer minAttendees,
        Long groupId,
        AttendanceSummaryDto attendanceSummaryDto,
        AttendanceStatus myStatus
) {
    public static EventListResponse from(Event event, AttendanceSummaryDto summaryDto, AttendanceStatus myStatus) {
        return new EventListResponse(
                event.getId(),
                event.getTitle(),
                event.getStartTime(),
                event.getType(),
                event.getMinAttendees(),
                event.getGroup().getId(),
                summaryDto,
                myStatus
        );
    }
}
