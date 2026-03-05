package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.model.GroupMember;

public record GroupCreatorDto(
        Long id,
        String username
) {
    public static GroupCreatorDto from (GroupMember groupMember) {
        return new GroupCreatorDto(
                groupMember.getId(),
                groupMember.getUser().getUsername()
        );
    }
}
