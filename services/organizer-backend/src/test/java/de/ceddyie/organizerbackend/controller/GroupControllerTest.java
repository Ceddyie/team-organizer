package de.ceddyie.organizerbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ceddyie.organizerbackend.dto.AttendanceSummaryDto;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.GroupMemberDto;
import de.ceddyie.organizerbackend.dto.requests.EventCreateRequest;
import de.ceddyie.organizerbackend.dto.requests.GroupCreateRequest;
import de.ceddyie.organizerbackend.dto.requests.GroupJoinRequest;
import de.ceddyie.organizerbackend.dto.responses.*;
import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.enums.EventType;
import de.ceddyie.organizerbackend.exceptions.ConflictException;
import de.ceddyie.organizerbackend.exceptions.ForbiddenException;
import de.ceddyie.organizerbackend.exceptions.GlobalExceptionHandler;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
import de.ceddyie.organizerbackend.service.EventService;
import de.ceddyie.organizerbackend.service.GroupService;
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
public class GroupControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GroupService groupService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private GroupController groupController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(groupController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(1L))
                .build();
    }

    @Test
    void createGroup_returnsOkWithResponse() throws Exception {
        var now = LocalDateTime.now();
        var creatorDto = new GroupCreatorDto(1L, "ceddy");
        var response = new GroupCreateResponse(10L, "CS Team", "ABCD1234", now, creatorDto, 1);
        var request = new GroupCreateRequest("CS Team", "discord webhook test");

        when(groupService.createGroup(1L, request)).thenReturn(response);

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GroupCreateRequest("CS Team", "discord webhook test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("CS Team"))
                .andExpect(jsonPath("$.inviteCode").value("ABCD1234"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.createdBy.username").value("ceddy"));

        verify(groupService).createGroup(1L, request);
    }

    @Test
    void joinGroup_returnsOkWithResponse() throws Exception {
        var now = LocalDateTime.now();
        var response = new GroupJoinResponse(5L, "CS Team", "ABCD1234", 2, now);

        when(groupService.joinGroup(1L, "ABCD1234")).thenReturn(response);

        mockMvc.perform(post("/api/groups/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GroupJoinRequest("ABCD1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CS Team"))
                .andExpect(jsonPath("$.memberCount").value(2));
    }

    @Test
    void joinGroup_returns409_whenAlreadyMember() throws Exception {
        when(groupService.joinGroup(1L, "ABCD1234"))
                .thenThrow(new ConflictException("Already a member"));

        mockMvc.perform(post("/api/groups/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GroupJoinRequest("ABCD1234"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already a member"));
    }

    @Test
    void getGroups_returnsListOfGroups() throws Exception {
        var now = LocalDateTime.now();
        var items = List.of(
                new GroupListResponseItem(1L, "Group A", "CODE1234", 3, now),
                new GroupListResponseItem(2L, "Group B", "CODE5678", 5, now)
        );

        when(groupService.getGroups(1L)).thenReturn(items);

        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Group A"))
                .andExpect(jsonPath("$[1].name").value("Group B"));
    }

    @Test
    void getGroupById_returnsGroupDetails() throws Exception {
        var now = LocalDateTime.now();
        var creatorDto = new GroupCreatorDto(1L, "ceddy");
        var members = List.of(
                new GroupMemberDto(1L, "ceddy", null, now),
                new GroupMemberDto(2L, "mate", "avatar.png", now)
        );
        var response = new GroupDetailResponse(10L, "CS Team", "ABCD1234", now, creatorDto, members, 2);

        when(groupService.getGroupById(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/api/groups/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.memberCount").value(2));
    }

    @Test
    void getGroupById_returns404_whenGroupNotFound() throws Exception {
        when(groupService.getGroupById(1L, 999L))
                .thenThrow(new ResourceNotFoundException("No group with ID 999"));

        mockMvc.perform(get("/api/groups/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No group with ID 999"));
    }

    @Test
    void getGroupById_returns403_whenUserNotMember() throws Exception {
        when(groupService.getGroupById(1L, 10L))
                .thenThrow(new ForbiddenException("User 1 is not member of group 10"));

        mockMvc.perform(get("/api/groups/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User 1 is not member of group 10"));
    }

    @Test
    void leaveGroup_returnsSuccessMessage() throws Exception {
        when(groupService.leaveGroup(1L, 10L))
                .thenReturn(new GroupLeaveResponse("Successfully left group"));

        mockMvc.perform(delete("/api/groups/10/leave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully left group"));
    }

    @Test
    void leaveGroup_returns403_whenCreatorTriesToLeave() throws Exception {
        when(groupService.leaveGroup(1L, 10L))
                .thenThrow(new ForbiddenException("Creator can't leave his group"));

        mockMvc.perform(delete("/api/groups/10/leave"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteGroup_returnsSuccessMessage() throws Exception {
        when(groupService.deleteGroup(1L, 10L))
                .thenReturn(new GroupLeaveResponse("Successfully deleted group"));

        mockMvc.perform(delete("/api/groups/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully deleted group"));
    }

    @Test
    void deleteGroup_returns403_whenNotCreator() throws Exception {
        when(groupService.deleteGroup(1L, 10L))
                .thenThrow(new ForbiddenException("User is not creator of group"));

        mockMvc.perform(delete("/api/groups/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void kickMember_returnsUpdatedGroupDetails() throws Exception {
        var now = LocalDateTime.now();
        var creatorDto = new GroupCreatorDto(1L, "ceddy");
        var members = List.of(new GroupMemberDto(1L, "ceddy", null, now));
        var response = new GroupDetailResponse(10L, "CS Team", "ABCD1234", now, creatorDto, members, 1);

        when(groupService.kickMember(1L, 10L, 2L)).thenReturn(response);

        mockMvc.perform(patch("/api/groups/10/kick/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].username").value("ceddy"));
    }

    @Test
    void updateGroup_returnsOkWithResponse() throws Exception {
        var now = LocalDateTime.now();
        var creatorDto = new GroupCreatorDto(1L, "ceddy");
        var request = new GroupCreateRequest("New Name", "https://webhook");
        var response = new GroupCreateResponse(10L, "New Name", "ABCD1234", now, creatorDto, 1);

        when(groupService.updateGroup(eq(1L), eq(10L), any(GroupCreateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/groups/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void createEvent_returnsOkWithResponse() throws Exception {
        var futureTime = LocalDateTime.now().plusDays(7);
        var now = LocalDateTime.now();
        var request = new EventCreateRequest("CS Match", futureTime, EventType.SINGLE, 5, "Match");
        var creatorDto = new GroupCreatorDto(1L, "ceddy");
        var summary = new AttendanceSummaryDto(0, 0, 0, 2);
        var response = new EventCreateResponse(100L, "CS Match", futureTime, EventType.SINGLE, 5, "Match", 10L, creatorDto, now, summary);

        when(eventService.createEvent(eq(1L), eq(10L), any(EventCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/groups/10/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("CS Match"))
                .andExpect(jsonPath("$.groupId").value(10));
    }

    @Test
    void getEventsOfGroup_returnsList() throws Exception {
        var futureTime = LocalDateTime.now().plusDays(7);
        var summary = new AttendanceSummaryDto(3, 0, 1, 1);
        var events = List.of(
                new EventListResponse(100L, "CS Match", futureTime, EventType.SINGLE, 5, 10L, summary, AttendanceStatus.ACCEPTED)
        );

        when(eventService.getEventsOfGroup(1L, 10L)).thenReturn(events);

        mockMvc.perform(get("/api/groups/10/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("CS Match"))
                .andExpect(jsonPath("$[0].myStatus").value("ACCEPTED"));
    }

    @Test
    void getEventsOfGroup_returns403_whenNotMember() throws Exception {
        when(eventService.getEventsOfGroup(1L, 10L))
                .thenThrow(new ForbiddenException("User is not member of group"));

        mockMvc.perform(get("/api/groups/10/events"))
                .andExpect(status().isForbidden());
    }
}
