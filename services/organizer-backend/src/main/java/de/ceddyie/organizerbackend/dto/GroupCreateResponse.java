package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.model.Group;

import java.time.LocalDateTime;

public record GroupCreateResponse(
        Long id,
        String name,
        String inviteCode,
        LocalDateTime createdAt,
        GroupCreatorDto createdBy,
        Integer memberCount
) {
    public static GroupCreateResponse from (Group group, GroupCreatorDto createdBy) {
        return new GroupCreateResponse(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                group.getCreatedAt(),
                createdBy,
                group.getMemberCount()
        );
    }
}
