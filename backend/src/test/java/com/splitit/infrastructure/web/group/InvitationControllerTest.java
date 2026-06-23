package com.splitit.infrastructure.web.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitit.domain.group.exception.InvitationExpiredException;
import com.splitit.domain.group.exception.InvitationNotFoundException;
import com.splitit.domain.group.exception.InvitationNotPendingException;
import com.splitit.domain.group.model.InvitationStatus;
import com.splitit.domain.group.port.in.AcceptInvitationUseCase;
import com.splitit.domain.group.port.in.AcceptInvitationUseCase.AcceptResult;
import com.splitit.infrastructure.security.JwtAuthenticationEntryPoint;
import com.splitit.infrastructure.security.JwtAuthenticationFilter;
import com.splitit.infrastructure.security.JwtTokenProvider;
import com.splitit.infrastructure.security.SecurityConfig;
import com.splitit.infrastructure.web.shared.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InvitationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class})
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcceptInvitationUseCase acceptInvitationUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID token = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();

    @Test
    void accept_existingUser_returns200_joined() throws Exception {
        when(acceptInvitationUseCase.accept(token))
                .thenReturn(new AcceptResult(groupId, "Trip", "bob@example.com", false));

        mockMvc.perform(get("/api/invitations/" + token + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.requiresRegistration").value(false));
    }

    @Test
    void accept_noAccount_returns200_requiresRegistration() throws Exception {
        when(acceptInvitationUseCase.accept(token))
                .thenReturn(new AcceptResult(groupId, "Trip", "ghost@example.com", true));

        mockMvc.perform(get("/api/invitations/" + token + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresRegistration").value(true));
    }

    @Test
    void accept_unknownToken_returns404() throws Exception {
        when(acceptInvitationUseCase.accept(any())).thenThrow(new InvitationNotFoundException());

        mockMvc.perform(get("/api/invitations/" + token + "/accept"))
                .andExpect(status().isNotFound());
    }

    @Test
    void accept_alreadyAccepted_returns409() throws Exception {
        when(acceptInvitationUseCase.accept(any()))
                .thenThrow(new InvitationNotPendingException(InvitationStatus.ACCEPTED));

        mockMvc.perform(get("/api/invitations/" + token + "/accept"))
                .andExpect(status().isConflict());
    }

    @Test
    void accept_expired_returns410() throws Exception {
        when(acceptInvitationUseCase.accept(any())).thenThrow(new InvitationExpiredException());

        mockMvc.perform(get("/api/invitations/" + token + "/accept"))
                .andExpect(status().isGone());
    }
}
