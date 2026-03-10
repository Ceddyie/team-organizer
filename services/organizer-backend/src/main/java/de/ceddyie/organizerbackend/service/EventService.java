package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.AttendanceSummaryDto;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.requests.EventCreateRequest;
import de.ceddyie.organizerbackend.dto.responses.EventCreateResponse;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.exceptions.ForbiddenException;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
import de.ceddyie.organizerbackend.exceptions.UnauthorizedException;
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

    public EventCreateResponse createEvent(Long userId, Long groupId, EventCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) throw new ForbiddenException("User is not member of group");

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

        int accepted = attendanceRepository.countByEventIdAndStatus(savedEvent.getId(), AttendanceStatus.ACCEPTED);
        int declined = attendanceRepository.countByEventIdAndStatus(savedEvent.getId(), AttendanceStatus.DECLINED);
        int maybe  = attendanceRepository.countByEventIdAndStatus(savedEvent.getId(), AttendanceStatus.TENTATIVE);
        int noResponse = attendanceRepository.countByEventIdAndStatus(savedEvent.getId(), AttendanceStatus.PENDING);

        return EventCreateResponse.from(savedEvent, GroupCreatorDto.from(savedEvent.getCreatedBy()), new AttendanceSummaryDto(accepted, declined, maybe, noResponse));
    }
}
