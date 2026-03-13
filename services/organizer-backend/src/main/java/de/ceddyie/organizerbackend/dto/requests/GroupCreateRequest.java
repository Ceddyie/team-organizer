package de.ceddyie.organizerbackend.dto.requests;

public record GroupCreateRequest(
        String name,
        String discordWebhookUrl
) {
}
