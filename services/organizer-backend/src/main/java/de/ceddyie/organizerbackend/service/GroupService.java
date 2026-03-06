package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.responses.GroupCreateResponse;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.responses.GroupJoinResponse;
import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.repository.GroupMemberRepository;
import de.ceddyie.organizerbackend.repository.GroupRepository;
import de.ceddyie.organizerbackend.repository.UserRepository;
import de.ceddyie.organizerbackend.util.InviteCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private static final int MAX_RETRIES = 5;

    private String generateInviteCode() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String code = InviteCodeGenerator.generate();
            if (!groupRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Failed to generate unique invite code after " + MAX_RETRIES + " attempts."
        );
    }

    public ResponseEntity<?> createGroup(Long userId, String name) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User creator = user.get();

        Group newGroup = new Group();
        newGroup.setName(name);
        newGroup.setCreatedBy(creator);
        newGroup.setCreatedAt(LocalDateTime.now());
        newGroup.setInviteCode(generateInviteCode());

        GroupMember founderMember = new GroupMember();
        founderMember.setGroup(newGroup);
        founderMember.setUser(creator);
        founderMember.setJoinedAt(LocalDateTime.now());

        newGroup.getMembers().add(founderMember);

        groupRepository.save(newGroup);

        GroupCreatorDto creatorDto = GroupCreatorDto.from(founderMember);
        return ResponseEntity.ok().body(GroupCreateResponse.from(newGroup, creatorDto));
    }

    public ResponseEntity<?> joinGroup(Long userId, String inviteCode) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = userOptional.get();

        Optional<Group> groupOptional = groupRepository.findByInviteCode(inviteCode);
        if (groupOptional.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Group group = groupOptional.get();

        if (groupMemberRepository.existsByGroupAndUser(group, user)) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        GroupMember newMember = new GroupMember();
        newMember.setGroup(group);
        newMember.setUser(user);
        newMember.setJoinedAt(LocalDateTime.now());

        groupMemberRepository.save(newMember);

        return ResponseEntity.ok().body(GroupJoinResponse.from(group, newMember));
    }
}
