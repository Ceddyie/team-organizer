package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.requests.EventCreateRequest;
import de.ceddyie.organizerbackend.dto.responses.EventCreateResponse;
import de.ceddyie.organizerbackend.dto.responses.EventDetailResponse;
import de.ceddyie.organizerbackend.dto.responses.EventListResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupLeaveResponse;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.exceptions.BadRequestException;
import de.ceddyie.organizerbackend.exceptions.ForbiddenException;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
import de.ceddyie.organizerbackend.exceptions.UnauthorizedException;
import de.ceddyie.organizerbackend.model.*;
import de.ceddyie.organizerbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private EventRepository eventRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private DiscordNotificationService discordNotificationService;

    @InjectMocks
    private EventService eventService;

    private User creator;
    private User member;
    private Group group;
    private Event event;
    private LocalDateTime futureTime;

    @BeforeEach
    void setUp() {
        futureTime = LocalDateTime.now().plusDays(7);

        creator = new User();
        creator.setId(1L);
        creator.setUsername("ceddy");
        creator.setDiscordId("discord-1");
        creator.setCreatedAt(LocalDateTime.now());

        member = new User();
        member.setId(2L);
        member.setUsername("mate");
        member.setDiscordId("discord-2");
        member.setCreatedAt(LocalDateTime.now());

        group = new Group();
        group.setId(10L);
        group.setName("CS Team");
        group.setInviteCode("ABCD1234");
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());

        event = new Event();
        event.setId(100L);
        event.setTitle("CS Match");
        event.setStartTime(futureTime);
        event.setType(EventType.SINGLE);
        event.setMinAttendees(5);
        event.setDescription("Competitive match");
        event.setGroup(group);
        event.setCreatedBy(creator);
        event.setCreatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // createEvent
    // =========================================================================
    @Nested
    class CreateEvent {

        @Test
        void createsEventAndAutoEnrollsMembers() {
            var request = new EventCreateRequest("CS Match", futureTime, EventType.SINGLE, 5, "Competitive match");

            GroupMember gm1 = new GroupMember();
            gm1.setId(1L);
            gm1.setUser(creator);
            gm1.setGroup(group);

            GroupMember gm2 = new GroupMember();
            gm2.setId(2L);
            gm2.setUser(member);
            gm2.setGroup(group);

            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
                Event e = inv.getArgument(0);
                e.setId(100L);
                e.setCreatedAt(LocalDateTime.now());
                return e;
            });
            when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(gm1, gm2));
            when(attendanceRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(attendanceRepository.countByEventIdAndStatus(eq(100L), any())).thenReturn(0);

            EventCreateResponse response = eventService.createEvent(1L, 10L, request);

            assertEquals(100L, response.id());
            assertEquals("CS Match", response.title());
            assertEquals(EventType.SINGLE, response.type());
            assertEquals(5, response.minAttendees());
            assertEquals(10L, response.groupId());
            assertEquals("ceddy", response.createdBy().username());

            verify(eventRepository).save(any(Event.class));
            verify(attendanceRepository).saveAll(anyList());
            verify(discordNotificationService).notifyEventCreated(eq(group), any(Event.class));
        }

        @Test
        void throwsUnauthorized_whenUserNotFound() {
            var request = new EventCreateRequest("CS Match", futureTime, EventType.SINGLE, 5, "Desc");
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> eventService.createEvent(999L, 10L, request));
            verify(eventRepository, never()).save(any());
        }

        @Test
        void throwsNotFound_whenGroupNotFound() {
            var request = new EventCreateRequest("CS Match", futureTime, EventType.SINGLE, 5, "Desc");
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> eventService.createEvent(1L, 999L, request));
        }

        @Test
        void throwsForbidden_whenUserNotMember() {
            var request = new EventCreateRequest("CS Match", futureTime, EventType.SINGLE, 5, "Desc");
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);

            assertThrows(ForbiddenException.class, () -> eventService.createEvent(2L, 10L, request));
        }

        @Test
        void throwsBadRequest_whenStartTimeInPast() {
            var pastTime = LocalDateTime.now().minusDays(1);
            var request = new EventCreateRequest("CS Match", pastTime, EventType.SINGLE, 5, "Desc");
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);

            assertThrows(BadRequestException.class, () -> eventService.createEvent(1L, 10L, request));
        }

        @Test
        void throwsBadRequest_whenMinAttendeesZeroOrNegative() {
            var request = new EventCreateRequest("CS Match", futureTime, EventType.SINGLE, 0, "Desc");
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);

            assertThrows(BadRequestException.class, () -> eventService.createEvent(1L, 10L, request));
        }
    }

    // =========================================================================
    // getEventsOfGroup
    // =========================================================================
    @Nested
    class GetEventsOfGroup {

        @Test
        void returnsListOfEvents() {
            EventAttendee attendee = new EventAttendee();
            attendee.setId(1L);
            attendee.setEvent(event);
            attendee.setUser(creator);
            attendee.setStatus(AttendanceStatus.ACCEPTED);

            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);
            when(eventRepository.findAllByGroupId(10L)).thenReturn(List.of(event));
            when(attendanceRepository.countByEventIdAndStatus(eq(100L), any())).thenReturn(0);
            when(attendanceRepository.findByEventIdAndUserId(100L, 1L)).thenReturn(Optional.of(attendee));

            List<EventListResponse> responses = eventService.getEventsOfGroup(1L, 10L);

            assertEquals(1, responses.size());
            assertEquals("CS Match", responses.get(0).title());
            assertEquals(AttendanceStatus.ACCEPTED, responses.get(0).myStatus());
        }

        @Test
        void returnsEmptyList_whenNoEvents() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);
            when(eventRepository.findAllByGroupId(10L)).thenReturn(List.of());

            List<EventListResponse> responses = eventService.getEventsOfGroup(1L, 10L);

            assertTrue(responses.isEmpty());
        }

        @Test
        void throwsUnauthorized_whenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(UnauthorizedException.class, () -> eventService.getEventsOfGroup(999L, 10L));
        }

        @Test
        void throwsNotFound_whenGroupNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> eventService.getEventsOfGroup(1L, 999L));
        }

        @Test
        void throwsForbidden_whenNotMember() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);

            assertThrows(ForbiddenException.class, () -> eventService.getEventsOfGroup(2L, 10L));
        }
    }

    // =========================================================================
    // getEventDetails
    // =========================================================================
    @Nested
    class GetEventDetails {

        @Test
        void returnsEventDetail() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);
            when(attendanceRepository.findAllByEventId(100L)).thenReturn(List.of());
            when(attendanceRepository.countByEventIdAndStatus(eq(100L), any())).thenReturn(0);

            EventDetailResponse response = eventService.getEventDetails(1L, 100L);

            assertEquals(100L, response.id());
            assertEquals("CS Match", response.title());
            assertEquals("CS Team", response.group().name());
            assertEquals("ceddy", response.createdBy().username());
        }

        @Test
        void throwsNotFound_whenEventNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> eventService.getEventDetails(1L, 999L));
        }

        @Test
        void throwsForbidden_whenUserNotMemberOfEventGroup() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);

            assertThrows(ForbiddenException.class, () -> eventService.getEventDetails(2L, 100L));
        }
    }

    // =========================================================================
    // updateEvent
    // =========================================================================
    @Nested
    class UpdateEvent {

        @Test
        void updatesEventAndNotifiesDiscord() {
            var newTime = LocalDateTime.now().plusDays(14);
            var request = new EventCreateRequest("Updated Match", newTime, EventType.SCHEDULED, 3, "Updated desc");

            // Creator is both event creator AND group creator -> condition passes
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
            when(attendanceRepository.findAllByEventId(100L)).thenReturn(List.of());
            when(attendanceRepository.countByEventIdAndStatus(eq(100L), any())).thenReturn(0);

            EventDetailResponse response = eventService.updateEvent(1L, 100L, request);

            assertEquals("Updated Match", response.title());
            assertEquals(EventType.SCHEDULED, response.type());
            assertEquals(3, response.minAttendees());
            verify(discordNotificationService).notifyEventUpdated(group, event);
        }

        @Test
        void throwsBadRequest_whenStartTimeInPast() {
            var pastTime = LocalDateTime.now().minusDays(1);
            var request = new EventCreateRequest("Match", pastTime, EventType.SINGLE, 5, "Desc");
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);

            assertThrows(BadRequestException.class, () -> eventService.updateEvent(1L, 100L, request));
        }

        @Test
        void throwsForbidden_whenUserNotMember() {
            var request = new EventCreateRequest("Match", futureTime, EventType.SINGLE, 5, "Desc");
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);

            assertThrows(ForbiddenException.class, () -> eventService.updateEvent(2L, 100L, request));
        }

        @Test
        void throwsForbidden_whenUserNotCreatorOfEventOrGroup() {
            // member is neither event creator nor group creator
            var request = new EventCreateRequest("Match", futureTime, EventType.SINGLE, 5, "Desc");
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(true);

            assertThrows(ForbiddenException.class, () -> eventService.updateEvent(2L, 100L, request));
        }
    }

    // =========================================================================
    // deleteEvent
    // =========================================================================
    @Nested
    class DeleteEvent {

        @Test
        void deletesEventAndNotifiesDiscord() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, creator)).thenReturn(true);

            GroupLeaveResponse response = eventService.deleteEvent(1L, 100L);

            assertEquals("Event deleted successfully!", response.message());
            verify(eventRepository).deleteById(100L);
            verify(discordNotificationService).notifyEventCancelled(group, event);
        }

        @Test
        void throwsUnauthorized_whenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(UnauthorizedException.class, () -> eventService.deleteEvent(999L, 100L));
        }

        @Test
        void throwsNotFound_whenEventNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> eventService.deleteEvent(1L, 999L));
        }

        @Test
        void throwsForbidden_whenUserNotMember() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);

            assertThrows(ForbiddenException.class, () -> eventService.deleteEvent(2L, 100L));
        }

        @Test
        void throwsForbidden_whenUserNotCreatorOfEventOrGroup() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(member));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(true);

            assertThrows(ForbiddenException.class, () -> eventService.deleteEvent(2L, 100L));
        }
    }
}
