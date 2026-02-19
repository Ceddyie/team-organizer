package de.ceddyie.organizerbackend.config;

import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.repository.UserRepository;
import de.ceddyie.organizerbackend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess (HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String discordId = oAuth2User.getAttribute("id");
        String username = oAuth2User.getAttribute("username");

        User user = userRepository.findByDiscordId(discordId)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth"));

        String token = jwtUtil.generateToken(user.getDiscordId(), user.getId(), user.getUsername());

        String redirectUrl = frontendUrl + "/auth/callback?token=" + token;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
