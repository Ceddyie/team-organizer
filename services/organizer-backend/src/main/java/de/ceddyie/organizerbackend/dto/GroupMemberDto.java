package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;

import java.time.LocalDateTime;

public record GroupMemberDto(
        Long id,
        String username,
        String avatar,
        LocalDateTime joinedAt
) {
    public static GroupMemberDto from(GroupMember groupMember) {
        return new GroupMemberDto(
                groupMember.getId(),
                groupMember.getUser().getUsername(),
                groupMember.getUser().getAvatar(),
                groupMember.getJoinedAt()
        );
    }
}
