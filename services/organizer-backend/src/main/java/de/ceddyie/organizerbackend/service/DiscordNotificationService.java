package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.config.DiscordWebhookClient;
import de.ceddyie.organizerbackend.dto.discord.DiscordEmbedDto;
import de.ceddyie.organizerbackend.dto.discord.DiscordFieldDto;
import de.ceddyie.organizerbackend.dto.discord.DiscordMessageDto;
import de.ceddyie.organizerbackend.model.Event;
import de.ceddyie.organizerbackend.model.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DiscordNotificationService {
    @Autowired
    private DiscordWebhookClient webhookClient;

    public void notifyEventCreated(Group group, Event event) {
        if (group.getDiscordWebhookUrl() == null) return;

        DiscordMessageDto message = DiscordMessageDto.of(
                "📅 New Event created!",
                DiscordEmbedDto.of(
                        event.getTitle(),
                        event.getDescription(),
                        DiscordEmbedDto.COLOR_BLUE,
                        DiscordFieldDto.inline("Date", formatDate(event.getStartTime())),
                        DiscordFieldDto.inline("Time", formatTime(event.getStartTime())),
                        DiscordFieldDto.block("Created by", event.getCreatedBy().getUsername())
                )
        );

        webhookClient.send(group.getDiscordWebhookUrl(), message);
    }

    public void notifyEventUpdated(Group group, Event event) {
        if (group.getDiscordWebhookUrl() == null) return;

        DiscordMessageDto message = DiscordMessageDto.of(
                "✏️ Event has been updated!",
                DiscordEmbedDto.of(
                        event.getTitle(),
                        event.getDescription(),
                        DiscordEmbedDto.COLOR_GREEN,
                        DiscordFieldDto.inline("Date", formatDate(event.getStartTime())),
                        DiscordFieldDto.inline("Time", formatTime(event.getStartTime()))
                )
        );

        webhookClient.send(group.getDiscordWebhookUrl(), message);
    }

    public void notifyEventCancelled(Group group, Event event) {
        if (group.getDiscordWebhookUrl() == null) return;

        DiscordMessageDto message = DiscordMessageDto.of(
                "❌ Event has been cancelled!",
                DiscordEmbedDto.of(
                        event.getTitle(),
                        "Event has been cancelled...",
                        DiscordEmbedDto.COLOR_RED
                )
        );

        webhookClient.send(group.getDiscordWebhookUrl(), message);
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String formatTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " h";
    }
}
