package de.ceddyie.organizerbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ceddyie.organizerbackend.dto.GroupCreatorDto;
import de.ceddyie.organizerbackend.dto.GroupMemberDto;
import de.ceddyie.organizerbackend.dto.requests.GroupCreateRequest;
import de.ceddyie.organizerbackend.dto.requests.GroupJoinRequest;
import de.ceddyie.organizerbackend.dto.responses.*;
import de.ceddyie.organizerbackend.exceptions.ConflictException;
import de.ceddyie.organizerbackend.exceptions.ForbiddenException;
import de.ceddyie.organizerbackend.exceptions.GlobalExceptionHandler;
import de.ceddyie.organizerbackend.exceptions.ResourceNotFoundException;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
public class GroupControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GroupService groupService;

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
}
