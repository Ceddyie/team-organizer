package de.ceddyie.organizerbackend.controller;

import de.ceddyie.organizerbackend.dto.requests.GroupCreateRequest;
import de.ceddyie.organizerbackend.dto.requests.GroupJoinRequest;
import de.ceddyie.organizerbackend.service.GroupService;
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
        log.info("User with ID {} creating a group with name {}", userId, request.name());
        return groupService.createGroup(userId, request.name());
    }

    @PostMapping("/join")
    private ResponseEntity<?> joinGroup(@AuthenticationPrincipal Long userId, @RequestBody GroupJoinRequest request) {
        log.info("User with ID {} trying to join group with invite code {}", userId, request.inviteCode());
        return groupService.joinGroup(userId, request.inviteCode());
    }
}
