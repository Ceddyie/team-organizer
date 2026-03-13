package de.ceddyie.organizerbackend.dto.discord;

import java.util.List;

public record DiscordMessageDto(
        String username,
        String content,
        List<DiscordEmbedDto> embeds
) {
    public static DiscordMessageDto of(String content, DiscordEmbedDto... embeds) {
        return new DiscordMessageDto("Team Organizer", content, List.of(embeds));
    }
}
