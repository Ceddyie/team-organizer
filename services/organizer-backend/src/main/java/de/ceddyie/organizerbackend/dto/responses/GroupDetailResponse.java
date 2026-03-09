package de.ceddyie.organizerbackend.dto.responses;

import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.GroupMemberDto;
import de.ceddyie.organizerbackend.model.Group;

import java.time.LocalDateTime;
import java.util.List;

public record GroupDetailResponse(
        Long id,
        String name,
        String inviteCode,
        LocalDateTime createdAt,
        GroupCreatorDto createdBy,
        List<GroupMemberDto> members,
        Integer memberCount
) {
    public static GroupDetailResponse from(Group group, GroupCreatorDto createdBy, List<GroupMemberDto> memberDtoList) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                group.getCreatedAt(),
                createdBy,
                memberDtoList,
                group.getMemberCount()
        );
    }
}
