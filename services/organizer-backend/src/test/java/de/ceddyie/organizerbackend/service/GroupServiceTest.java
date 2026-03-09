package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.responses.GroupCreateResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupDetailResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupJoinResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupLeaveResponse;
import de.ceddyie.organizerbackend.exceptions.ConflictException;
import de.ceddyie.organizerbackend.exceptions.ForbiddenException;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.repository.GroupMemberRepository;
import de.ceddyie.organizerbackend.repository.GroupRepository;
import de.ceddyie.organizerbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private GroupService groupService;

    private User creator;
    private User member;
    private Group group;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("ceddy");
        creator.setDiscordId("discord-1");
        creator.setCreatedAt(LocalDateTime.now());

        member = new User();
        member.setId(2L);
        member.setUsername("mate");
        member.setDiscordId("discord-2");
        member.setAvatar("avatar.png");
        member.setCreatedAt(LocalDateTime.now());

        group = new Group();
        group.setId(10L);
        group.setName("Weekend Trip");
        group.setInviteCode("ABCDEFGH");
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createGroup_createsFounderAndReturnsResponse() {
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(groupRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group savedGroup = invocation.getArgument(0);
            savedGroup.setId(99L);
            GroupMember founder = savedGroup.getMembers().iterator().next();
            founder.setId(123L);
            return savedGroup;
        });

        GroupCreateResponse response = groupService.createGroup(creator.getId(), "Trip Planners");

        assertEquals(99L, response.id());
        assertEquals("Trip Planners", response.name());
        assertNotNull(response.inviteCode());
        assertEquals(8, response.inviteCode().length());
        assertEquals(1, response.memberCount());
        assertEquals(123L, response.createdBy().id());
        assertEquals("ceddy", response.createdBy().username());

        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void joinGroup_throwsConflict_whenUserAlreadyMember() {
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(groupRepository.findByInviteCode("ABCDEFGH")).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(true);

        assertThrows(ConflictException.class, () -> groupService.joinGroup(member.getId(), "ABCDEFGH"));

        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    void joinGroup_savesMembership_andReturnsResponse() {
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(groupRepository.findByInviteCode("ABCDEFGH")).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> {
            GroupMember gm = invocation.getArgument(0);
            gm.setId(55L);
            return gm;
        });

        GroupJoinResponse response = groupService.joinGroup(member.getId(), "ABCDEFGH");

        assertEquals(55L, response.id());
        assertEquals("Weekend Trip", response.name());
        assertEquals("ABCDEFGH", response.inviteCode());
        assertNotNull(response.joinedAt());
        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    @Test
    void getGroupById_throwsForbidden_whenUserIsNotMember() {
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> groupService.getGroupById(member.getId(), group.getId()));
    }

    @Test
    void leaveGroup_throwsForbidden_whenCreatorTriesToLeave() {
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThrows(ForbiddenException.class, () -> groupService.leaveGroup(creator.getId(), group.getId()));

        verify(groupMemberRepository, never()).deleteByGroupAndUser(any(), any());
    }

    @Test
    void deleteGroup_deletesMembershipsAndGroup_whenRequesterIsCreator() {
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        GroupLeaveResponse response = groupService.deleteGroup(creator.getId(), group.getId());

        assertEquals("Successfully deleted group", response.message());
        verify(groupMemberRepository).deleteByGroup(group);
        verify(groupRepository).deleteById(group.getId());
    }

    @Test
    void kickMember_removesMember_andReturnsUpdatedGroupDetails() {
        GroupMember storedMember = new GroupMember();
        storedMember.setId(77L);
        storedMember.setGroup(group);
        storedMember.setUser(member);
        storedMember.setJoinedAt(LocalDateTime.now().minusDays(2));

        GroupMember creatorMembership = new GroupMember();
        creatorMembership.setId(1L);
        creatorMembership.setGroup(group);
        creatorMembership.setUser(creator);
        creatorMembership.setJoinedAt(LocalDateTime.now().minusDays(5));

        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(group.getId(), member.getId())).thenReturn(Optional.of(storedMember));
        when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(creatorMembership));

        GroupDetailResponse response = groupService.kickMember(creator.getId(), group.getId(), member.getId());

        assertEquals(group.getId(), response.id());
        assertEquals(creator.getId(), response.createdBy().id());
        assertEquals(1, response.members().size());
        assertEquals("ceddy", response.members().getFirst().username());
        verify(groupMemberRepository).deleteByGroupAndUser(group, member);
    }

    @Test
    void kickMember_throwsNotFound_whenTargetMembershipDoesNotExist() {
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(group.getId(), member.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> groupService.kickMember(creator.getId(), group.getId(), member.getId()));
    }
}

