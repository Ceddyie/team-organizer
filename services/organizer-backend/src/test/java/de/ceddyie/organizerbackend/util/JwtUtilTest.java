package de.ceddyie.organizerbackend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);
    }

    @Test
    void generateToken_extractsAllExpectedClaims() {
        String token = jwtUtil.generateToken("discord-123", 42L, "Cedric");

        assertEquals("discord-123", jwtUtil.extractDiscordId(token));
        assertEquals(42L, jwtUtil.extractUserId(token));
        assertEquals("Cedric", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.extractExpiration(token).after(new Date()));
    }

    @Test
    void validateToken_withExpectedDiscordId_returnsTrue() {
        String token = jwtUtil.generateToken("discord-123", 42L, "Cedric");

        assertTrue(jwtUtil.validateToken(token, "discord-123"));
        assertFalse(jwtUtil.validateToken(token, "different-id"));
    }

    @Test
    void validateToken_withoutExpectedDiscordId_returnsFalse_forBrokenToken() {
        assertFalse(jwtUtil.validateToken("definitely-not-a-jwt"));
    }
}
