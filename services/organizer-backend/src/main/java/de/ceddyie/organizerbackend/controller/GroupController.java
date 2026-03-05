package de.ceddyie.organizerbackend.controller;

import de.ceddyie.organizerbackend.dto.GroupCreateRequest;
import de.ceddyie.organizerbackend.dto.GroupCreateResponse;
import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.service.GroupService;
import de.ceddyie.organizerbackend.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http:/localhost:5173/")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @PostMapping
    private ResponseEntity<?> createGroup(@AuthenticationPrincipal Long userId, @RequestBody GroupCreateRequest request) {
        log.info("User ID: {}", userId);
        return groupService.createGroup(userId, request.name());
    }
}
