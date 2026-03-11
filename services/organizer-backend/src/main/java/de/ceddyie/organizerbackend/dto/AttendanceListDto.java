package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.model.EventAttendee;

import java.time.LocalDateTime;

public record AttendanceListDto(
        GroupMemberDto user,
        AttendanceStatus status,
        String comment,
        LocalDateTime updatedAt
) {
    public static AttendanceListDto from(EventAttendee eventAttendee) {
        return new AttendanceListDto(
                GroupMemberDto.from(eventAttendee.getUser()),
                eventAttendee.getStatus(),
                eventAttendee.getComment(),
                eventAttendee.getUpdatedAt()
        );
    }
}
