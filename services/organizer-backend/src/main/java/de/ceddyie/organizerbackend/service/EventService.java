package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.AttendanceListDto;
import de.ceddyie.organizerbackend.dto.AttendanceSummaryDto;
import de.ceddyie.organizerbackend.dto.EventGroupDto;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.requests.EventCreateRequest;
import de.ceddyie.organizerbackend.dto.responses.EventCreateResponse;
import de.ceddyie.organizerbackend.dto.responses.EventDetailResponse;
import de.ceddyie.organizerbackend.dto.responses.EventListResponse;
import de.ceddyie.organizerbackend.dto.responses.GroupLeaveResponse;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.exceptions.*;
import de.ceddyie.organizerbackend.model.*;
import de.ceddyie.organizerbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private DiscordNotificationService discordNotificationService;

    private AttendanceSummaryDto getAttendanceSummary(Long id) {
        int accepted = attendanceRepository.countByEventIdAndStatus(id, AttendanceStatus.ACCEPTED);
        int declined = attendanceRepository.countByEventIdAndStatus(id, AttendanceStatus.DECLINED);
        int maybe  = attendanceRepository.countByEventIdAndStatus(id, AttendanceStatus.TENTATIVE);
        int noResponse = attendanceRepository.countByEventIdAndStatus(id, AttendanceStatus.PENDING);

        return new AttendanceSummaryDto(accepted, declined, maybe, noResponse);
    }

    private AttendanceStatus status(Long userId, Long eventId) {
        return attendanceRepository.findByEventIdAndUserId(eventId, userId).map(EventAttendee::getStatus)
                .orElseThrow(() -> new ResourceNotFoundException("Attendee not found"));
    }

    public EventCreateResponse createEvent(Long userId, Long groupId, EventCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) throw new ForbiddenException("User is not member of group");

        if (request.startTime().isBefore(LocalDateTime.now()) || request.minAttendees() <= 0) throw new BadRequestException("Entered values are not allowed");

        Event event = new Event();
        event.setTitle(request.title());
        event.setStartTime(request.startTime());
        event.setType(request.type());
        event.setMinAttendees(request.minAttendees());
        event.setDescription(request.description());
        event.setCreatedBy(user);
        event.setGroup(group);

        Event savedEvent = eventRepository.save(event);

        List<GroupMember> members = groupMemberRepository.findByGroup(group);

        List<EventAttendee> attendees = members.stream()
                .map(member -> {
                        EventAttendee attendee = new EventAttendee();
                        attendee.setEvent(savedEvent);
                        attendee.setUser(member.getUser());
                        attendee.setUpdatedAt(LocalDateTime.now());
                        return attendee;
                    }).toList();

        attendanceRepository.saveAll(attendees);

        AttendanceSummaryDto attendanceSummaryDto = getAttendanceSummary(savedEvent.getId());

        discordNotificationService.notifyEventCreated(group, savedEvent);

        return EventCreateResponse.from(savedEvent, GroupCreatorDto.from(savedEvent.getCreatedBy()), attendanceSummaryDto);
    }

    public List<EventListResponse> getEventsOfGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group does not exist"));

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) throw new ForbiddenException("User is not member of group");

        return eventRepository.findAllByGroupId(groupId).stream()
                .map(e -> EventListResponse.from(e, getAttendanceSummary(e.getId()), status(userId, e.getId())))
                .toList();
    }

    public EventDetailResponse getEventDetails(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event does not exist"));

        if (!groupMemberRepository.existsByGroupAndUser(event.getGroup(), user)) throw new ForbiddenException("User is not member of group");

        EventGroupDto eventGroupDto = EventGroupDto.from(event.getGroup());
        GroupCreatorDto groupCreatorDto = GroupCreatorDto.from(event.getCreatedBy());
        List<AttendanceListDto> attendanceListDtos = attendanceRepository.findAllByEventId(eventId).stream()
                .map(AttendanceListDto::from).toList();

        return EventDetailResponse.from(event, eventGroupDto, groupCreatorDto, attendanceListDtos, getAttendanceSummary(eventId));
    }

    public EventDetailResponse updateEvent(Long userId, Long eventId, EventCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event does not exist"));

        if (!groupMemberRepository.existsByGroupAndUser(event.getGroup(), user)) throw new ForbiddenException("User is not member of group");
        if (!event.getCreatedBy().getId().equals(user.getId()) || !event.getGroup().getCreatedBy().getId().equals(user.getId())) throw new ForbiddenException("User is not creator of event or group");
        if (request.startTime().isBefore(LocalDateTime.now()) || request.minAttendees() <= 0) throw new BadRequestException("Values are not valid");

        event.setTitle(request.title());
        event.setStartTime(request.startTime());
        event.setType(request.type());
        event.setMinAttendees(request.minAttendees());
        event.setDescription(request.description());

        Event savedEvent = eventRepository.save(event);
        EventGroupDto eventGroupDto = EventGroupDto.from(savedEvent.getGroup());
        GroupCreatorDto groupCreatorDto = GroupCreatorDto.from(savedEvent.getCreatedBy());
        List<AttendanceListDto> attendanceListDtos = attendanceRepository.findAllByEventId(savedEvent.getId()).stream()
                .map(AttendanceListDto::from).toList();

        discordNotificationService.notifyEventUpdated(savedEvent.getGroup(), savedEvent);

        return EventDetailResponse.from(savedEvent, eventGroupDto, groupCreatorDto, attendanceListDtos, getAttendanceSummary(savedEvent.getId()));
    }

    public GroupLeaveResponse deleteEvent(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event does not exist"));

        if (!groupMemberRepository.existsByGroupAndUser(event.getGroup(), user)) throw new ForbiddenException("User is not member of group");
        if (!event.getCreatedBy().getId().equals(user.getId()) || !event.getGroup().getCreatedBy().getId().equals(user.getId())) throw new ForbiddenException("User is not creator of event or group");

        eventRepository.deleteById(eventId);

        discordNotificationService.notifyEventCancelled(event.getGroup(), event);

        return new GroupLeaveResponse("Event deleted successfully!");
    }
}
