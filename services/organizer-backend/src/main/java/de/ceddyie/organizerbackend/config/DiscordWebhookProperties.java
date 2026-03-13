package de.ceddyie.organizerbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@ConfigurationProperties(prefix = "discord.webhook")
@Component
public class DiscordWebhookProperties {
    private boolean enabled = true;
    private int timeOutMs = 5000;
    private String username = "Team Organizer";
}
