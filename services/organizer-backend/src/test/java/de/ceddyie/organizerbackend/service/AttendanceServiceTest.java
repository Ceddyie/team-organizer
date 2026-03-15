package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.requests.AttendanceRequest;
import de.ceddyie.organizerbackend.dto.responses.AttendanceDetailResponse;
import de.ceddyie.organizerbackend.dto.responses.AttendanceResponse;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.enums.EventType;
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
class AttendanceServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private EventRepository eventRepository;
    @Mock private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private User user;
    private Group group;
    private Event event;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("ceddy");
        user.setDiscordId("discord-1");
        user.setCreatedAt(LocalDateTime.now());

        group = new Group();
        group.setId(10L);
        group.setName("CS Team");
        group.setInviteCode("ABCD1234");
        group.setCreatedBy(user);
        group.setCreatedAt(LocalDateTime.now());

        event = new Event();
        event.setId(100L);
        event.setTitle("CS Match");
        event.setStartTime(LocalDateTime.now().plusDays(7));
        event.setType(EventType.SINGLE);
        event.setMinAttendees(5);
        event.setDescription("Match");
        event.setGroup(group);
        event.setCreatedBy(user);
        event.setCreatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // updateAttendance
    // =========================================================================
    @Nested
    class UpdateAttendance {

        @Test
        void updatesExistingAttendee() {
            var request = new AttendanceRequest(AttendanceStatus.ACCEPTED, "I'm in!");

            EventAttendee existing = new EventAttendee();
            existing.setId(50L);
            existing.setEvent(event);
            existing.setUser(user);
            existing.setStatus(AttendanceStatus.PENDING);
            existing.setUpdatedAt(LocalDateTime.now().minusDays(1));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, user)).thenReturn(true);
            when(attendanceRepository.findByEventIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any(EventAttendee.class))).thenAnswer(inv -> inv.getArgument(0));

            AttendanceResponse response = attendanceService.updateAttendance(1L, 100L, request);

            assertEquals(50L, response.id());
            assertEquals(AttendanceStatus.ACCEPTED, response.status());
            assertEquals("I'm in!", response.comment());
            verify(attendanceRepository).save(existing);
        }

        @Test
        void createsNewAttendee_whenNoneExists() {
            var request = new AttendanceRequest(AttendanceStatus.DECLINED, "Can't make it");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, user)).thenReturn(true);
            when(attendanceRepository.findByEventIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
            when(attendanceRepository.save(any(EventAttendee.class))).thenAnswer(inv -> {
                EventAttendee a = inv.getArgument(0);
                a.setId(99L);
                return a;
            });

            AttendanceResponse response = attendanceService.updateAttendance(1L, 100L, request);

            assertEquals(99L, response.id());
            assertEquals(AttendanceStatus.DECLINED, response.status());
            assertEquals("Can't make it", response.comment());
        }

        @Test
        void throwsUnauthorized_whenUserNotFound() {
            var request = new AttendanceRequest(AttendanceStatus.ACCEPTED, null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> attendanceService.updateAttendance(999L, 100L, request));
        }

        @Test
        void throwsNotFound_whenEventNotFound() {
            var request = new AttendanceRequest(AttendanceStatus.ACCEPTED, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> attendanceService.updateAttendance(1L, 999L, request));
        }

        @Test
        void throwsUnauthorized_whenUserNotMemberOfGroup() {
            var request = new AttendanceRequest(AttendanceStatus.ACCEPTED, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, user)).thenReturn(false);

            assertThrows(UnauthorizedException.class, () -> attendanceService.updateAttendance(1L, 100L, request));
            verify(attendanceRepository, never()).save(any());
        }

        @Test
        void updatesWithTentativeStatus() {
            var request = new AttendanceRequest(AttendanceStatus.TENTATIVE, "Maybe");

            EventAttendee existing = new EventAttendee();
            existing.setId(50L);
            existing.setEvent(event);
            existing.setUser(user);
            existing.setStatus(AttendanceStatus.PENDING);
            existing.setUpdatedAt(LocalDateTime.now());

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, user)).thenReturn(true);
            when(attendanceRepository.findByEventIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any(EventAttendee.class))).thenAnswer(inv -> inv.getArgument(0));

            AttendanceResponse response = attendanceService.updateAttendance(1L, 100L, request);

            assertEquals(AttendanceStatus.TENTATIVE, response.status());
            assertEquals("Maybe", response.comment());
        }
    }

    // =========================================================================
    // getEventAttendance
    // =========================================================================
    @Nested
    class GetEventAttendance {

        @Test
        void returnsAttendanceDetailWithSummary() {
            EventAttendee attendee = new EventAttendee();
            attendee.setId(50L);
            attendee.setEvent(event);
            attendee.setUser(user);
            attendee.setStatus(AttendanceStatus.ACCEPTED);
            attendee.setComment("Ready");
            attendee.setUpdatedAt(LocalDateTime.now());

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, user)).thenReturn(true);
            when(attendanceRepository.findAllByEventId(100L)).thenReturn(List.of(attendee));
            when(attendanceRepository.countByEventIdAndStatus(100L, AttendanceStatus.ACCEPTED)).thenReturn(1);
            when(attendanceRepository.countByEventIdAndStatus(100L, AttendanceStatus.DECLINED)).thenReturn(0);
            when(attendanceRepository.countByEventIdAndStatus(100L, AttendanceStatus.TENTATIVE)).thenReturn(0);
            when(attendanceRepository.countByEventIdAndStatus(100L, AttendanceStatus.PENDING)).thenReturn(0);

            AttendanceDetailResponse response = attendanceService.getEventAttendance(1L, 100L);

            assertEquals(100L, response.eventId());
            assertEquals(1, response.summary().accepted());
            assertEquals(0, response.summary().declined());
            assertEquals(1, response.attendances().size());
            assertEquals(AttendanceStatus.ACCEPTED, response.attendances().get(0).status());
        }

        @Test
        void returnsEmptyAttendances_whenNoneExist() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, user)).thenReturn(true);
            when(attendanceRepository.findAllByEventId(100L)).thenReturn(List.of());
            when(attendanceRepository.countByEventIdAndStatus(eq(100L), any())).thenReturn(0);

            AttendanceDetailResponse response = attendanceService.getEventAttendance(1L, 100L);

            assertEquals(100L, response.eventId());
            assertTrue(response.attendances().isEmpty());
            assertEquals(0, response.summary().accepted());
        }

        @Test
        void throwsUnauthorized_whenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(UnauthorizedException.class, () -> attendanceService.getEventAttendance(999L, 100L));
        }

        @Test
        void throwsNotFound_whenEventNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> attendanceService.getEventAttendance(1L, 999L));
        }

        @Test
        void throwsUnauthorized_whenUserNotMember() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
            when(groupMemberRepository.existsByGroupAndUser(group, user)).thenReturn(false);

            assertThrows(UnauthorizedException.class, () -> attendanceService.getEventAttendance(1L, 100L));
        }
    }
}
