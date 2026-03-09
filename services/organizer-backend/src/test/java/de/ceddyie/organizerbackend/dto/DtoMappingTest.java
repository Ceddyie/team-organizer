package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.dto.responses.*;
import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoMappingTest {

    private User user;
    private Group group;
    private GroupMember groupMember;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2025, 3, 15, 10, 0);

        user = new User();
        user.setId(1L);
        user.setUsername("ceddy");
        user.setAvatar("avatar.png");
        user.setDiscordId("discord-1");
        user.setCreatedAt(now);

        group = new Group();
        group.setId(10L);
        group.setName("CS Team");
        group.setInviteCode("ABCD1234");
        group.setCreatedAt(now);
        group.setCreatedBy(user);

        groupMember = new GroupMember();
        groupMember.setId(100L);
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setJoinedAt(now);
    }

    @Test
    void groupCreatorDto_fromGroupMember_mapsCorrectly() {
        GroupCreatorDto dto = GroupCreatorDto.from(groupMember);

        assertEquals(100L, dto.id());
        assertEquals("ceddy", dto.username());
    }

    @Test
    void groupCreatorDto_fromUser_mapsCorrectly() {
        GroupCreatorDto dto = GroupCreatorDto.from(user);

        assertEquals(1L, dto.id());
        assertEquals("ceddy", dto.username());
    }

    @Test
    void groupMemberDto_fromGroupMember_mapsCorrectly() {
        GroupMemberDto dto = GroupMemberDto.from(groupMember);

        assertEquals(100L, dto.id());
        assertEquals("ceddy", dto.username());
        assertEquals("avatar.png", dto.avatar());
        assertEquals(now, dto.joinedAt());
    }

    @Test
    void groupCreateResponse_from_mapsCorrectly() {
        group.getMembers().add(groupMember);
        GroupCreatorDto creatorDto = new GroupCreatorDto(1L, "ceddy");

        GroupCreateResponse response = GroupCreateResponse.from(group, creatorDto);

        assertEquals(10L, response.id());
        assertEquals("CS Team", response.name());
        assertEquals("ABCD1234", response.inviteCode());
        assertEquals(now, response.createdAt());
        assertEquals(1, response.memberCount());
        assertEquals("ceddy", response.createdBy().username());
    }

    @Test
    void groupDetailResponse_from_mapsCorrectly() {
        GroupCreatorDto creatorDto = new GroupCreatorDto(1L, "ceddy");
        List<GroupMemberDto> members = List.of(GroupMemberDto.from(groupMember));

        group.getMembers().add(groupMember);
        GroupDetailResponse response = GroupDetailResponse.from(group, creatorDto, members);

        assertEquals(10L, response.id());
        assertEquals("CS Team", response.name());
        assertEquals(1, response.members().size());
        assertEquals(1, response.memberCount());
        assertEquals("ceddy", response.createdBy().username());
    }

    @Test
    void groupJoinResponse_from_mapsCorrectly() {
        GroupJoinResponse response = GroupJoinResponse.from(group, groupMember);

        assertEquals(100L, response.id());
        assertEquals("CS Team", response.name());
        assertEquals("ABCD1234", response.inviteCode());
        assertEquals(now, response.joinedAt());
    }

    @Test
    void groupListResponseItem_from_mapsCorrectly() {
        group.getMembers().add(groupMember);
        GroupListResponseItem item = GroupListResponseItem.from(group, groupMember);

        assertEquals(10L, item.id());
        assertEquals("CS Team", item.name());
        assertEquals("ABCD1234", item.inviteCode());
        assertEquals(1, item.memberCount());
        assertEquals(now, item.joinedAt());
    }

    @Test
    void group_memberCount_returnsZero_whenNoMembers() {
        assertEquals(0, group.getMemberCount());
    }

    @Test
    void errorResponse_holdsMessage() {
        ErrorResponse response = new ErrorResponse("something went wrong");
        assertEquals("something went wrong", response.message());
    }

    @Test
    void groupLeaveResponse_holdsMessage() {
        GroupLeaveResponse response = new GroupLeaveResponse("left successfully");
        assertEquals("left successfully", response.message());
    }
}
