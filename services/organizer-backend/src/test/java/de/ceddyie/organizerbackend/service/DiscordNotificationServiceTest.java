package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.config.DiscordWebhookClient;
import de.ceddyie.organizerbackend.dto.discord.DiscordMessageDto;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.model.Event;
import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordNotificationServiceTest {

    @Mock
    private DiscordWebhookClient webhookClient;

    @InjectMocks
    private DiscordNotificationService discordNotificationService;

    private Group group;
    private Event event;
    private User creator;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("ceddy");
        creator.setDiscordId("discord-1");
        creator.setCreatedAt(LocalDateTime.now());

        group = new Group();
        group.setId(10L);
        group.setName("CS Team");
        group.setDiscordWebhookUrl("https://discord.com/api/webhooks/test");
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());

        event = new Event();
        event.setId(100L);
        event.setTitle("CS Match");
        event.setStartTime(LocalDateTime.of(2025, 6, 15, 20, 0));
        event.setType(EventType.SINGLE);
        event.setMinAttendees(5);
        event.setDescription("Competitive match tonight");
        event.setGroup(group);
        event.setCreatedBy(creator);
        event.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    class NotifyEventCreated {

        @Test
        void sendsWebhookWithCorrectContent() {
            discordNotificationService.notifyEventCreated(group, event);

            ArgumentCaptor<DiscordMessageDto> captor = ArgumentCaptor.forClass(DiscordMessageDto.class);
            verify(webhookClient).send(eq("https://discord.com/api/webhooks/test"), captor.capture());

            DiscordMessageDto message = captor.getValue();
            assertTrue(message.content().contains("New Event created"));
            assertEquals("Team Organizer", message.username());
            assertEquals(1, message.embeds().size());
            assertEquals("CS Match", message.embeds().get(0).title());
            assertEquals("Competitive match tonight", message.embeds().get(0).description());
        }

        @Test
        void doesNotSend_whenWebhookUrlIsNull() {
            group.setDiscordWebhookUrl(null);

            discordNotificationService.notifyEventCreated(group, event);

            verify(webhookClient, never()).send(anyString(), any());
        }
    }

    @Nested
    class NotifyEventUpdated {

        @Test
        void sendsWebhookWithUpdateContent() {
            discordNotificationService.notifyEventUpdated(group, event);

            ArgumentCaptor<DiscordMessageDto> captor = ArgumentCaptor.forClass(DiscordMessageDto.class);
            verify(webhookClient).send(eq("https://discord.com/api/webhooks/test"), captor.capture());

            DiscordMessageDto message = captor.getValue();
            assertTrue(message.content().contains("updated"));
            assertEquals("CS Match", message.embeds().get(0).title());
        }

        @Test
        void doesNotSend_whenWebhookUrlIsNull() {
            group.setDiscordWebhookUrl(null);

            discordNotificationService.notifyEventUpdated(group, event);

            verify(webhookClient, never()).send(anyString(), any());
        }
    }

    @Nested
    class NotifyEventCancelled {

        @Test
        void sendsWebhookWithCancelContent() {
            discordNotificationService.notifyEventCancelled(group, event);

            ArgumentCaptor<DiscordMessageDto> captor = ArgumentCaptor.forClass(DiscordMessageDto.class);
            verify(webhookClient).send(eq("https://discord.com/api/webhooks/test"), captor.capture());

            DiscordMessageDto message = captor.getValue();
            assertTrue(message.content().contains("cancelled"));
            assertEquals("CS Match", message.embeds().get(0).title());
            assertTrue(message.embeds().get(0).description().contains("cancelled"));
        }

        @Test
        void doesNotSend_whenWebhookUrlIsNull() {
            group.setDiscordWebhookUrl(null);

            discordNotificationService.notifyEventCancelled(group, event);

            verify(webhookClient, never()).send(anyString(), any());
        }
    }

    @Test
    void embedFieldsContainFormattedDateAndTime() {
        discordNotificationService.notifyEventCreated(group, event);

        ArgumentCaptor<DiscordMessageDto> captor = ArgumentCaptor.forClass(DiscordMessageDto.class);
        verify(webhookClient).send(anyString(), captor.capture());

        var fields = captor.getValue().embeds().get(0).fields();
        // Date field: 15.06.2025
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("Date") && f.value().equals("15.06.2025")));
        // Time field: 20:00 h
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("Time") && f.value().equals("20:00 h")));
        // Created by field
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("Created by") && f.value().equals("ceddy")));
    }
}
