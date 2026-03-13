package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.GroupMemberDto;
import de.ceddyie.organizerbackend.dto.requests.GroupCreateRequest;
import de.ceddyie.organizerbackend.dto.responses.*;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.exceptions.ConflictException;
import de.ceddyie.organizerbackend.exceptions.ForbiddenException;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
import de.ceddyie.organizerbackend.exceptions.UnauthorizedException;
import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.repository.GroupMemberRepository;
import de.ceddyie.organizerbackend.repository.GroupRepository;
import de.ceddyie.organizerbackend.repository.UserRepository;
import de.ceddyie.organizerbackend.util.InviteCodeGenerator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    public GroupCreateResponse createGroup(Long userId, GroupCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group newGroup = new Group();
        newGroup.setName(request.name());
        newGroup.setCreatedBy(user);
        newGroup.setCreatedAt(LocalDateTime.now());
        newGroup.setInviteCode(generateInviteCode());
        newGroup.setDiscordWebhookUrl(request.discordWebhookUrl());

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

    public GroupCreateResponse updateGroup(Long userId, Long groupId, GroupCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group does not exist"));

        if (!group.getCreatedBy().getId().equals(user.getId())) throw new UnauthorizedException("User is not creator of group");

        group.setName(request.name());
        group.setDiscordWebhookUrl(request.discordWebhookUrl());

        Group updatedGroup = groupRepository.save(group);

        return GroupCreateResponse.from(updatedGroup, GroupCreatorDto.from(group.getCreatedBy()));
    }

    public List<GroupListResponseItem> getGroups(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        return groupMemberRepository.findAllByUser(user).stream()
                .map(gm -> GroupListResponseItem.from(gm.getGroup(), gm))
                .toList();
    }

    public GroupDetailResponse getGroupById(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No group with ID " + groupId));

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) throw new ForbiddenException("User " + userId + " is not member of group " + groupId);

        List<GroupMemberDto> memberDtoList = group.getMembers().stream()
                .map(gm -> new GroupMemberDto(
                        gm.getUser().getId(),
                        gm.getUser().getUsername(),
                        gm.getUser().getAvatar(),
                        gm.getJoinedAt()
                ))
                .toList();

        return GroupDetailResponse.from(group, GroupCreatorDto.from(group.getCreatedBy()), memberDtoList);
    }

    @Transactional
    public GroupLeaveResponse leaveGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No group with ID " + groupId));

        if (group.getCreatedBy().getId().equals(user.getId())) throw new ForbiddenException("Creator can't leave his group");

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) throw new ForbiddenException("User is no member of group");

        groupMemberRepository.deleteByGroupAndUser(group, user);

        return new GroupLeaveResponse("Successfully left group");
    }

    @Transactional
    public GroupLeaveResponse deleteGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No group with ID " + groupId));

        if (!group.getCreatedBy().getId().equals(user.getId())) throw new ForbiddenException("User is not creator of group");

        groupMemberRepository.deleteByGroup(group);
        groupRepository.deleteById(groupId);

        return new GroupLeaveResponse("Successfully deleted group");
    }

    @Transactional
    public GroupDetailResponse kickMember(Long userId, Long groupId, Long memberId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with ID " + memberId + " found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No group with ID " + groupId));

        GroupMember groupMember = groupMemberRepository.findByGroupIdAndUserId(groupId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("No member found with ID " + memberId + " in group " + groupId));

        if (!group.getCreatedBy().getId().equals(user.getId())) throw new ForbiddenException("User is not creator of group");
        if (group.getCreatedBy().getId().equals(groupMember.getUser().getId())) throw new ForbiddenException("Creator of the group cannot be kicked");

        groupMemberRepository.deleteByGroupAndUser(group, member);

        GroupCreatorDto creatorDto = GroupCreatorDto.from(group.getCreatedBy());

        List<GroupMemberDto> memberDtoList = groupMemberRepository.findByGroup(group).stream()
                .map(GroupMemberDto::from).toList();

        return GroupDetailResponse.from(group, creatorDto, memberDtoList);
    }
}
