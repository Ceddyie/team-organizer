package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.model.Group;

public record EventGroupDto(
        Long id,
        String name
) {
    public static EventGroupDto from(Group group) {
        return new EventGroupDto(
                group.getId(),
                group.getName()
        );
    }
}
