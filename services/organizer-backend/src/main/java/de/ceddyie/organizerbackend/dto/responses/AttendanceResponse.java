package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.dto.EventGroupDto;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.GroupMemberDto;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.model.EventAttendee;

import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        GroupCreatorDto user,
        EventGroupDto event,
        AttendanceStatus status,
        String comment,
        LocalDateTime updatedAt
) {
    public static AttendanceResponse from (EventAttendee attendee) {
        return new AttendanceResponse(
                attendee.getId(),
                GroupCreatorDto.from(attendee.getUser()),
                EventGroupDto.from(attendee.getEvent()),
                attendee.getStatus(),
                attendee.getComment(),
                attendee.getUpdatedAt()
        );
    }
}
