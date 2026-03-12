package de.ceddyie.organizerbackend.dto.requests;

import de.ceddyie.organizerbackend.enums.AttendanceStatus;

public record AttendanceRequest(
        AttendanceStatus status,
        String comment
) {
}
