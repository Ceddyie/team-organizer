package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;

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

    public static GroupCreatorDto from (User user) {
        return new GroupCreatorDto(
                user.getId(),
                user.getUsername()
        );
    }
}
