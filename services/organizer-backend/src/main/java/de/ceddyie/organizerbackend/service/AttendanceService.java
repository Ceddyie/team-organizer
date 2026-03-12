package de.ceddyie.organizerbackend.service;

import de.ceddyie.organizerbackend.dto.requests.AttendanceRequest;
import de.ceddyie.organizerbackend.dto.responses.AttendanceResponse;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
import de.ceddyie.organizerbackend.exceptions.UnauthorizedException;
import de.ceddyie.organizerbackend.model.Event;
import de.ceddyie.organizerbackend.model.EventAttendee;
import de.ceddyie.organizerbackend.model.User;
import de.ceddyie.organizerbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AttendanceService {
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


    public AttendanceResponse updateAttendance(Long userId, Long eventId, AttendanceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event does not exist"));

        if (!groupMemberRepository.existsByGroupAndUser(event.getGroup(), user)) throw new UnauthorizedException("User is not member of group");

        EventAttendee attendee;

        Optional<EventAttendee> attendeeOptional = attendanceRepository.findByEventIdAndUserId(eventId, userId);
        if (attendeeOptional.isPresent()) {
            attendee = attendeeOptional.get();
        } else {
            attendee = new EventAttendee();
            attendee.setUser(user);
            attendee.setEvent(event);
        }

        attendee.setStatus(request.status());
        attendee.setComment(request.comment());
        attendee.setUpdatedAt(LocalDateTime.now());

        EventAttendee savedAttendee = attendanceRepository.save(attendee);

        return AttendanceResponse.from(savedAttendee);
    }
}

