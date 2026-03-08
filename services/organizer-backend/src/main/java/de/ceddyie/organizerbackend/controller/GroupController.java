package de.ceddyie.organizerbackend.controller;

import de.ceddyie.organizerbackend.dto.requests.GroupCreateRequest;
import de.ceddyie.organizerbackend.dto.requests.GroupJoinRequest;
import de.ceddyie.organizerbackend.dto.responses.GroupCreateResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupDetailResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupJoinResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupListResponseItem;
import de.ceddyie.organizerbackend.service.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http:/localhost:5173/")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @PostMapping
    private ResponseEntity<GroupCreateResponse> createGroup(@AuthenticationPrincipal Long userId, @RequestBody GroupCreateRequest request) {
        log.info("User with ID {} creating a group with name {}", userId, request.name());
        return ResponseEntity.ok(groupService.createGroup(userId, request.name()));
    }

    @PostMapping("/join")
    private ResponseEntity<GroupJoinResponse> joinGroup(@AuthenticationPrincipal Long userId, @RequestBody GroupJoinRequest request) {
        log.info("User with ID {} trying to join group with invite code {}", userId, request.inviteCode());
        return ResponseEntity.ok(groupService.joinGroup(userId, request.inviteCode()));
    }

    @GetMapping
    private ResponseEntity<List<GroupListResponseItem>> getGroups(@AuthenticationPrincipal Long userId) {
        log.info("User with ID {} requesting a list of his groups", userId);
        return ResponseEntity.ok(groupService.getGroups(userId));
    }

    @GetMapping("/{groupId}")
    private ResponseEntity<GroupDetailResponse> getGroupById(@AuthenticationPrincipal Long userId, @PathVariable Long groupId) {
        log.info("User with ID {} requests details of group with ID {}", userId, groupId);
        return ResponseEntity.ok(groupService.getGroupById(userId, groupId));
    }
}
