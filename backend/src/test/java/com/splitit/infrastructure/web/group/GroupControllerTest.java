package com.splitit.infrastructure.web.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitit.domain.group.exception.AlreadyMemberException;
import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.group.model.Group;
import com.splitit.domain.group.model.GroupDetails;
import com.splitit.domain.group.model.GroupMember;
import com.splitit.domain.group.model.GroupSummary;
import com.splitit.domain.group.model.MemberRole;
import com.splitit.domain.group.port.in.InviteToGroupUseCase;
import com.splitit.domain.group.port.in.InviteToGroupUseCase.InviteResult;
import com.splitit.domain.group.port.in.ManageGroupsUseCase;
import com.splitit.infrastructure.security.AuthenticatedUser;
import com.splitit.infrastructure.security.JwtAuthenticationEntryPoint;
import com.splitit.infrastructure.security.JwtAuthenticationFilter;
import com.splitit.infrastructure.security.JwtTokenProvider;
import com.splitit.infrastructure.security.SecurityConfig;
import com.splitit.infrastructure.web.shared.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GroupController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class})
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManageGroupsUseCase manageGroupsUseCase;

    @MockBean
    private InviteToGroupUseCase inviteToGroupUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID userId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();

    private Authentication principal() {
        AuthenticatedUser user = new AuthenticatedUser(userId, "alice@example.com");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private GroupDetails sampleDetails() {
        Group group = new Group(groupId, "Trip", "Summer", userId, OffsetDateTime.now());
        return new GroupDetails(group,
                List.of(new GroupMember(userId, "Alice", "alice@example.com", MemberRole.OWNER)));
    }

    @Test
    void listGroups_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listGroups_authenticated_returnsList() throws Exception {
        when(manageGroupsUseCase.listGroups(userId)).thenReturn(List.of(
                new GroupSummary(groupId, "Trip", "Summer", MemberRole.OWNER, 3, OffsetDateTime.now())));

        mockMvc.perform(get("/api/groups").with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trip"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].membersCount").value(3));
    }

    @Test
    void createGroup_returns201WithDetails() throws Exception {
        when(manageGroupsUseCase.createGroup(eq(userId), any())).thenReturn(sampleDetails());

        mockMvc.perform(post("/api/groups").with(authentication(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Trip","description":"Summer"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Trip"))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"));
    }

    @Test
    void createGroup_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/groups").with(authentication(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  ","description":"x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void getGroup_nonMember_returns404() throws Exception {
        when(manageGroupsUseCase.getGroup(eq(userId), eq(groupId)))
                .thenThrow(new GroupNotFoundException(groupId));

        mockMvc.perform(get("/api/groups/" + groupId).with(authentication(principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void invite_existingUser_returnsAddedImmediately() throws Exception {
        when(inviteToGroupUseCase.invite(eq(userId), eq(groupId), eq("bob@example.com")))
                .thenReturn(new InviteResult(true));

        mockMvc.perform(post("/api/groups/" + groupId + "/invite").with(authentication(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bob@example.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addedImmediately").value(true));
    }

    @Test
    void invite_alreadyMember_returns409() throws Exception {
        when(inviteToGroupUseCase.invite(any(), any(), any()))
                .thenThrow(new AlreadyMemberException("bob@example.com"));

        mockMvc.perform(post("/api/groups/" + groupId + "/invite").with(authentication(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bob@example.com"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void invite_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/groups/" + groupId + "/invite").with(authentication(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email"}"""))
                .andExpect(status().isBadRequest());
    }
}
