package de.ceddyie.organizerbackend.config;

import de.ceddyie.organizerbackend.dto.discord.DiscordMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;


@Component
public class DiscordWebhookClient {
    private static final Logger log = LoggerFactory.getLogger(DiscordWebhookClient.class);

    private final RestClient restClient;
    private final DiscordWebhookProperties properties;

    public DiscordWebhookClient(DiscordWebhookProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().requestFactory(clientRequestFactory(properties.getTimeOutMs()))
                .build();
    }

    public void send(String webhookUrl, DiscordMessageDto message) {
        if (!properties.isEnabled()) {
            log.debug("Discord Webhook disabled, skipping notification");
            return;
        }

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Discord notification sent successfully");
        } catch (HttpClientErrorException e) {
            log.error("Discord rejected the request: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Discord server error: {}", e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Could not reach Discord (timeout/network): {}", e.getMessage());
        }
    }

    private ClientHttpRequestFactory clientRequestFactory(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return factory;
    }
}
