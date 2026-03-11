package de.ceddyie.organizerbackend.controller;

import de.ceddyie.organizerbackend.dto.responses.EventDetailResponse;
import de.ceddyie.organizerbackend.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173/")
public class EventController {
    @Autowired
    private EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEventDetails(@AuthenticationPrincipal Long userId, @PathVariable Long eventId) {
        log.info("User with ID {} requests details of event {}", userId, eventId);
        return ResponseEntity.ok(eventService.getEventDetails(userId, eventId));
    }
}
