package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.responses.GroupCreateResponse;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.responses.GroupJoinResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupListResponseItem;
import de.ceddyie.organizerbackend.exceptions.ConflictException;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
import de.ceddyie.organizerbackend.exceptions.UnauthorizedException;
import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.repository.GroupMemberRepository;
import de.ceddyie.organizerbackend.repository.GroupRepository;
import de.ceddyie.organizerbackend.repository.UserRepository;
import de.ceddyie.organizerbackend.util.InviteCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public GroupCreateResponse createGroup(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group newGroup = new Group();
        newGroup.setName(name);
        newGroup.setCreatedBy(user);
        newGroup.setCreatedAt(LocalDateTime.now());
        newGroup.setInviteCode(generateInviteCode());

        GroupMember founderMember = new GroupMember();
        founderMember.setGroup(newGroup);
        founderMember.setUser(user);
        founderMember.setJoinedAt(LocalDateTime.now());

        newGroup.getMembers().add(founderMember);

        groupRepository.save(newGroup);

        GroupCreatorDto creatorDto = GroupCreatorDto.from(founderMember);
        return GroupCreateResponse.from(newGroup, creatorDto);
    }

    public GroupJoinResponse joinGroup(Long userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("No group with Invite Code: " + inviteCode));

        if (groupMemberRepository.existsByGroupAndUser(group, user)) throw new ConflictException("User " + userId + " is already member of group " + group.getId());

        GroupMember newMember = new GroupMember();
        newMember.setGroup(group);
        newMember.setUser(user);
        newMember.setJoinedAt(LocalDateTime.now());

        groupMemberRepository.save(newMember);

        return GroupJoinResponse.from(group, newMember);
    }

    public List<GroupListResponseItem> getGroups(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        return groupMemberRepository.findAllByUser(user).stream()
                .map(gm -> GroupListResponseItem.from(gm.getGroup(), gm))
                .toList();
    }
}
