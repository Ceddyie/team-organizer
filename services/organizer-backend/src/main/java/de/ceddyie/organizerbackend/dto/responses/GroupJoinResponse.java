package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;

import java.time.LocalDateTime;

public record GroupJoinResponse(
        Long id,
        String name,
        String inviteCode,
        Integer memberCount,
        LocalDateTime joinedAt
) {
    public static GroupJoinResponse from (Group group, GroupMember groupMember) {
        return new GroupJoinResponse(
                groupMember.getId(),
                group.getName(),
                group.getInviteCode(),
                group.getMemberCount(),
                groupMember.getJoinedAt()
        );
    }
}
