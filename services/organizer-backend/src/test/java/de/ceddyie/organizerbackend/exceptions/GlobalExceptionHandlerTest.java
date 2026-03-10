package de.ceddyie.organizerbackend.exceptions;

import de.ceddyie.organizerbackend.dto.responses.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleUnauthorized_returns401() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(new UnauthorizedException("no auth"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("no auth", response.getBody().message());
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new ResourceNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", response.getBody().message());
    }

    @Test
    void handleConflict_returns409() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(new ConflictException("duplicate"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("duplicate", response.getBody().message());
    }

    @Test
    void handleForbidden_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(new ForbiddenException("forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("forbidden", response.getBody().message());
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new BadRequestException("bad"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad", response.getBody().message());
    }
}
