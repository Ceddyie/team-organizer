package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.dto.AttendanceListDto;
import de.ceddyie.organizerbackend.dto.AttendanceSummaryDto;
import de.ceddyie.organizerbackend.dto.EventGroupDto;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public record EventDetailResponse(
        Long id,
        String title,
        LocalDateTime startTime,
        EventType type,
        Integer minAttendees,
        String description,
        Long groupId,
        EventGroupDto group,
        GroupCreatorDto createdBy,
        LocalDateTime createdAt,
        List<AttendanceListDto> attendances,
        AttendanceSummaryDto attendanceSummary
) {
    public static EventDetailResponse from(Event event, EventGroupDto eventGroupDto, GroupCreatorDto groupCreatorDto, List<AttendanceListDto> attendanceListDtos, AttendanceSummaryDto attendanceSummary) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getStartTime(),
                event.getType(),
                event.getMinAttendees(),
                event.getDescription(),
                event.getGroup().getId(),
                eventGroupDto,
                groupCreatorDto,
                event.getCreatedAt(),
                attendanceListDtos,
                attendanceSummary
        );
    }
}
