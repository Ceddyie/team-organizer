package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.dto.AttendanceListDto;
import de.ceddyie.organizerbackend.dto.AttendanceSummaryDto;
import de.ceddyie.organizerbackend.model.EventAttendee;

import java.util.List;

public record AttendanceDetailResponse(
        Long eventId,
        AttendanceSummaryDto summary,
        List<AttendanceListDto> attendances
) {
    public static AttendanceDetailResponse from(Long eventId, AttendanceSummaryDto summaryDto, List<AttendanceListDto> attendances) {
        return new AttendanceDetailResponse(
                eventId,
                summaryDto,
                attendances
        );
    }
}
