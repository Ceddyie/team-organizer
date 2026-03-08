package de.ceddyie.organizerbackend.dto;

import java.time.LocalDateTime;

public record GroupMemberDto(
        Long id,
        String username,
        String avatar,
        LocalDateTime joinedAt
) {
}
