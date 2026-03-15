package de.ceddyie.organizerbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ceddyie.organizerbackend.dto.*;
import de.ceddyie.organizerbackend.dto.requests.AttendanceRequest;
import de.ceddyie.organizerbackend.dto.requests.EventCreateRequest;
import de.ceddyie.organizerbackend.dto.responses.*;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.exceptions.*;
import de.ceddyie.organizerbackend.service.AttendanceService;
import de.ceddyie.organizerbackend.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private EventService eventService;
    @Mock private AttendanceService attendanceService;

    @InjectMocks
    private EventController eventController;

    private LocalDateTime futureTime;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(1L))
                .build();
        futureTime = LocalDateTime.now().plusDays(7);
        now = LocalDateTime.now();
    }

    // =========================================================================
    // GET /api/events/{eventId}
    // =========================================================================
    @Test
    void getEventDetails_returnsOk() throws Exception {
        var response = new EventDetailResponse(
                100L, "CS Match", futureTime, EventType.SINGLE, 5, "Competitive",
                10L, new EventGroupDto(10L, "CS Team"), new GroupCreatorDto(1L, "ceddy"),
                now, List.of(), new AttendanceSummaryDto(2, 1, 0, 2)
        );

        when(eventService.getEventDetails(1L, 100L)).thenReturn(response);

        mockMvc.perform(get("/api/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("CS Match"))
                .andExpect(jsonPath("$.group.name").value("CS Team"))
                .andExpect(jsonPath("$.attendanceSummary.accepted").value(2));
    }

    @Test
    void getEventDetails_returns404_whenNotFound() throws Exception {
        when(eventService.getEventDetails(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Event does not exist"));

        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event does not exist"));
    }

    @Test
    void getEventDetails_returns403_whenNotMember() throws Exception {
        when(eventService.getEventDetails(1L, 100L))
                .thenThrow(new ForbiddenException("User is not member of group"));

        mockMvc.perform(get("/api/events/100"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // PUT /api/events/{eventId}
    // =========================================================================
    @Test
    void updateEvent_returnsOk() throws Exception {
        var request = new EventCreateRequest("Updated", futureTime, EventType.SCHEDULED, 3, "New desc");
        var response = new EventDetailResponse(
                100L, "Updated", futureTime, EventType.SCHEDULED, 3, "New desc",
                10L, new EventGroupDto(10L, "CS Team"), new GroupCreatorDto(1L, "ceddy"),
                now, List.of(), new AttendanceSummaryDto(0, 0, 0, 5)
        );

        when(eventService.updateEvent(eq(1L), eq(100L), any(EventCreateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.type").value("SCHEDULED"))
                .andExpect(jsonPath("$.minAttendees").value(3));
    }

    @Test
    void updateEvent_returns400_whenBadRequest() throws Exception {
        var request = new EventCreateRequest("Match", futureTime, EventType.SINGLE, 5, "Desc");

        when(eventService.updateEvent(eq(1L), eq(100L), any(EventCreateRequest.class)))
                .thenThrow(new BadRequestException("Values are not valid"));

        mockMvc.perform(put("/api/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // DELETE /api/events/{eventId}
    // =========================================================================
    @Test
    void deleteEvent_returnsOk() throws Exception {
        when(eventService.deleteEvent(1L, 100L))
                .thenReturn(new GroupLeaveResponse("Event deleted successfully!"));

        mockMvc.perform(delete("/api/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event deleted successfully!"));
    }

    @Test
    void deleteEvent_returns403_whenNotCreator() throws Exception {
        when(eventService.deleteEvent(1L, 100L))
                .thenThrow(new ForbiddenException("User is not creator of event or group"));

        mockMvc.perform(delete("/api/events/100"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/events/{eventId}/attendance
    // =========================================================================
    @Test
    void updateAttendance_returnsOk() throws Exception {
        var request = new AttendanceRequest(AttendanceStatus.ACCEPTED, "I'm in!");
        var response = new AttendanceResponse(
                50L, new GroupCreatorDto(1L, "ceddy"), new EventGroupDto(100L, "CS Match"),
                AttendanceStatus.ACCEPTED, "I'm in!", now
        );

        when(attendanceService.updateAttendance(eq(1L), eq(100L), any(AttendanceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/events/100/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.comment").value("I'm in!"));
    }

    @Test
    void updateAttendance_returns401_whenUnauthorized() throws Exception {
        var request = new AttendanceRequest(AttendanceStatus.ACCEPTED, null);

        when(attendanceService.updateAttendance(eq(1L), eq(100L), any(AttendanceRequest.class)))
                .thenThrow(new UnauthorizedException("User is not logged in"));

        mockMvc.perform(post("/api/events/100/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /api/events/{eventId}/attendance
    // =========================================================================
    @Test
    void getEventAttendance_returnsOk() throws Exception {
        var response = new AttendanceDetailResponse(
                100L,
                new AttendanceSummaryDto(3, 1, 1, 0),
                List.of()
        );

        when(attendanceService.getEventAttendance(1L, 100L)).thenReturn(response);

        mockMvc.perform(get("/api/events/100/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(100))
                .andExpect(jsonPath("$.summary.accepted").value(3))
                .andExpect(jsonPath("$.summary.declined").value(1));
    }

    @Test
    void getEventAttendance_returns404_whenEventNotFound() throws Exception {
        when(attendanceService.getEventAttendance(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Event does not exist"));

        mockMvc.perform(get("/api/events/999/attendance"))
                .andExpect(status().isNotFound());
    }
}
