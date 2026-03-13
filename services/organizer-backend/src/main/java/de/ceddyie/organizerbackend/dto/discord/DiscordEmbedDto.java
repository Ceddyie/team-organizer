package de.ceddyie.organizerbackend.dto.discord;

import java.util.List;

public record DiscordEmbedDto(
        String title,
        String description,
        int color,
        List<DiscordFieldDto> fields
) {
    public static final int COLOR_BLUE = 5814783;
    public static final int COLOR_GREEN = 3066993;
    public static final int COLOR_RED = 15158332;

    public static DiscordEmbedDto of(String title, String description, int color, DiscordFieldDto... fields) {
        return new DiscordEmbedDto(title, description, color, List.of(fields));
    }
}
