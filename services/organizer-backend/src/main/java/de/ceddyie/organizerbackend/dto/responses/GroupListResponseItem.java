package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;

import java.time.LocalDateTime;

public record GroupListResponseItem(
        Long id,
        String name,
        String inviteCode,
        Integer memberCount,
        LocalDateTime joinedAt
) {
    public static GroupListResponseItem from(Group group, GroupMember groupMember) {
        return new GroupListResponseItem(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                group.getMemberCount(),
                groupMember.getJoinedAt()
        );
    }
}
