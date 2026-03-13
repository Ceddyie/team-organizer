package de.ceddyie.organizerbackend.dto.discord;

public record DiscordFieldDto(
        String name,
        String value,
        boolean inline
) {
    public static DiscordFieldDto inline(String name, String value) {
        return new DiscordFieldDto(name, value, true);
    }

    public static DiscordFieldDto block(String name, String value) {
        return new DiscordFieldDto(name, value, false);
    }
}
