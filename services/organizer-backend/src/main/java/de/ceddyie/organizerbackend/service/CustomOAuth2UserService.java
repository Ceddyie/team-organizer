package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String discordId = oAuth2User.getAttribute("id");
        String username = oAuth2User.getAttribute("username");
        String avatar = oAuth2User.getAttribute("avatar");

        User user = userRepository.findByDiscordId(discordId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setDiscordId(discordId);
                    newUser.setCreatedAt(LocalDateTime.now());
                    return newUser;
                });

        user.setUsername(username);
        user.setAvatar(avatar);
        userRepository.save(user);

        return oAuth2User;
    }
}
