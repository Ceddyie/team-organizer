package de.ceddyie.organizerbackend.dto;

import de.ceddyie.organizerbackend.dto.discord.DiscordEmbedDto;
import de.ceddyie.organizerbackend.dto.discord.DiscordFieldDto;
import de.ceddyie.organizerbackend.dto.discord.DiscordMessageDto;
import de.ceddyie.organizerbackend.dto.responses.*;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.model.*;
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

    // =========================================================================
    // Event & Attendance DTO Mappings
    // =========================================================================

    @Test
    void eventGroupDto_fromGroup_mapsCorrectly() {
        EventGroupDto dto = EventGroupDto.from(group);
        assertEquals(10L, dto.id());
        assertEquals("CS Team", dto.name());
    }

    @Test
    void eventGroupDto_fromEvent_mapsCorrectly() {
        Event event = new Event();
        event.setId(50L);
        event.setTitle("Training");
        EventGroupDto dto = EventGroupDto.from(event);
        assertEquals(50L, dto.id());
        assertEquals("Training", dto.name());
    }

    @Test
    void groupMemberDto_fromUser_mapsCorrectly() {
        GroupMemberDto dto = GroupMemberDto.from(user);
        assertEquals(1L, dto.id());
        assertEquals("ceddy", dto.username());
        assertEquals("avatar.png", dto.avatar());
        assertEquals(now, dto.joinedAt());
    }

    @Test
    void attendanceListDto_from_mapsCorrectly() {
        EventAttendee attendee = new EventAttendee();
        attendee.setId(77L);
        attendee.setUser(user);
        attendee.setStatus(AttendanceStatus.ACCEPTED);
        attendee.setComment("Let's go!");
        attendee.setUpdatedAt(now);

        Event event = new Event();
        event.setId(50L);
        event.setGroup(group);
        attendee.setEvent(event);

        AttendanceListDto dto = AttendanceListDto.from(attendee);
        assertEquals(AttendanceStatus.ACCEPTED, dto.status());
        assertEquals("Let's go!", dto.comment());
        assertEquals(now, dto.updatedAt());
        assertEquals("ceddy", dto.user().username());
    }

    @Test
    void attendanceSummaryDto_holdsValues() {
        AttendanceSummaryDto dto = new AttendanceSummaryDto(3, 1, 2, 4);
        assertEquals(3, dto.accepted());
        assertEquals(1, dto.declined());
        assertEquals(2, dto.maybe());
        assertEquals(4, dto.noResponse());
    }

    @Test
    void attendanceResponse_from_mapsCorrectly() {
        Event event = new Event();
        event.setId(50L);
        event.setTitle("Match");
        event.setGroup(group);

        EventAttendee attendee = new EventAttendee();
        attendee.setId(77L);
        attendee.setUser(user);
        attendee.setEvent(event);
        attendee.setStatus(AttendanceStatus.TENTATIVE);
        attendee.setComment("Maybe");
        attendee.setUpdatedAt(now);

        AttendanceResponse response = AttendanceResponse.from(attendee);
        assertEquals(77L, response.id());
        assertEquals(AttendanceStatus.TENTATIVE, response.status());
        assertEquals("Maybe", response.comment());
        assertEquals("ceddy", response.user().username());
        assertEquals(50L, response.event().id());
    }

    @Test
    void attendanceDetailResponse_from_mapsCorrectly() {
        var summary = new AttendanceSummaryDto(1, 0, 0, 1);
        List<AttendanceListDto> attendances = List.of();
        AttendanceDetailResponse response = AttendanceDetailResponse.from(50L, summary, attendances);

        assertEquals(50L, response.eventId());
        assertEquals(1, response.summary().accepted());
        assertTrue(response.attendances().isEmpty());
    }

    @Test
    void eventCreateResponse_from_mapsCorrectly() {
        Event event = new Event();
        event.setId(50L);
        event.setTitle("Match");
        event.setStartTime(now);
        event.setType(EventType.SINGLE);
        event.setMinAttendees(5);
        event.setDescription("Desc");
        event.setGroup(group);
        event.setCreatedBy(user);
        event.setCreatedAt(now);

        var creatorDto = new GroupCreatorDto(1L, "ceddy");
        var summary = new AttendanceSummaryDto(0, 0, 0, 5);

        EventCreateResponse response = EventCreateResponse.from(event, creatorDto, summary);
        assertEquals(50L, response.id());
        assertEquals("Match", response.title());
        assertEquals(EventType.SINGLE, response.type());
        assertEquals(5, response.minAttendees());
        assertEquals(10L, response.groupId());
        assertEquals("ceddy", response.createdBy().username());
        assertEquals(5, response.attendanceSummary().noResponse());
    }

    @Test
    void eventDetailResponse_from_mapsCorrectly() {
        Event event = new Event();
        event.setId(50L);
        event.setTitle("Match");
        event.setStartTime(now);
        event.setType(EventType.SCHEDULED);
        event.setMinAttendees(3);
        event.setDescription("Weekly");
        event.setGroup(group);
        event.setCreatedBy(user);
        event.setCreatedAt(now);

        var eventGroupDto = new EventGroupDto(10L, "CS Team");
        var creatorDto = new GroupCreatorDto(1L, "ceddy");
        var summary = new AttendanceSummaryDto(2, 0, 1, 0);

        EventDetailResponse response = EventDetailResponse.from(event, eventGroupDto, creatorDto, List.of(), summary);
        assertEquals(50L, response.id());
        assertEquals("Match", response.title());
        assertEquals(EventType.SCHEDULED, response.type());
        assertEquals("CS Team", response.group().name());
        assertEquals("ceddy", response.createdBy().username());
        assertEquals(2, response.attendanceSummary().accepted());
        assertTrue(response.attendances().isEmpty());
    }

    @Test
    void eventListResponse_from_mapsCorrectly() {
        Event event = new Event();
        event.setId(50L);
        event.setTitle("Match");
        event.setStartTime(now);
        event.setType(EventType.SINGLE);
        event.setMinAttendees(5);
        event.setGroup(group);

        var summary = new AttendanceSummaryDto(3, 1, 0, 1);

        EventListResponse response = EventListResponse.from(event, summary, AttendanceStatus.ACCEPTED);
        assertEquals(50L, response.id());
        assertEquals("Match", response.title());
        assertEquals(5, response.minAttendees());
        assertEquals(10L, response.groupId());
        assertEquals(AttendanceStatus.ACCEPTED, response.myStatus());
        assertEquals(3, response.attendanceSummaryDto().accepted());
    }

    // =========================================================================
    // Discord DTOs
    // =========================================================================

    @Test
    void discordFieldDto_inline_setsInlineTrue() {
        DiscordFieldDto field = DiscordFieldDto.inline("Date", "15.06.2025");
        assertEquals("Date", field.name());
        assertEquals("15.06.2025", field.value());
        assertTrue(field.inline());
    }

    @Test
    void discordFieldDto_block_setsInlineFalse() {
        DiscordFieldDto field = DiscordFieldDto.block("Note", "Important");
        assertFalse(field.inline());
    }

    @Test
    void discordEmbedDto_of_createsWithFieldsAndColor() {
        DiscordEmbedDto embed = DiscordEmbedDto.of("Title", "Desc", DiscordEmbedDto.COLOR_BLUE,
                DiscordFieldDto.inline("A", "1"));
        assertEquals("Title", embed.title());
        assertEquals("Desc", embed.description());
        assertEquals(DiscordEmbedDto.COLOR_BLUE, embed.color());
        assertEquals(1, embed.fields().size());
    }

    @Test
    void discordMessageDto_of_createsWithUsername() {
        DiscordMessageDto msg = DiscordMessageDto.of("Hello",
                DiscordEmbedDto.of("T", "D", DiscordEmbedDto.COLOR_RED));
        assertEquals("Team Organizer", msg.username());
        assertEquals("Hello", msg.content());
        assertEquals(1, msg.embeds().size());
    }
}
