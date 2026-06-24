package com.splitit.infrastructure.web.settlement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.settlement.model.Settlement;
import com.splitit.domain.settlement.model.SettlementSuggestion;
import com.splitit.domain.settlement.port.in.SettlementUseCase;
import com.splitit.infrastructure.security.AuthenticatedUser;
import com.splitit.infrastructure.security.JwtTokenProvider;
import com.splitit.infrastructure.web.shared.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SettlementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SettlementUseCase settlementUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID GROUP_ID = UUID.randomUUID();
    private final UUID PAYER_ID = UUID.randomUUID();
    private final UUID PAYEE_ID = UUID.randomUUID();
    private final UUID CALLER_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        AuthenticatedUser principal = new AuthenticatedUser(CALLER_ID, "alice@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    // =========================================================================
    // GET /api/groups/{groupId}/settlements
    // =========================================================================

    @Test
    void suggest_plan_returns_200_with_list() throws Exception {
        SettlementSuggestion suggestion = new SettlementSuggestion(
                PAYER_ID, "Alice", PAYEE_ID, "Carol", new BigDecimal("30.00"));

        when(settlementUseCase.suggestPlan(eq(CALLER_ID), eq(GROUP_ID)))
                .thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/groups/{groupId}/settlements", GROUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payerName").value("Alice"))
                .andExpect(jsonPath("$[0].payeeName").value("Carol"))
                .andExpect(jsonPath("$[0].amount").value(30.00));
    }

    @Test
    void suggest_plan_returns_empty_list_when_all_settled() throws Exception {
        when(settlementUseCase.suggestPlan(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/groups/{groupId}/settlements", GROUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void suggest_plan_non_member_returns_404() throws Exception {
        when(settlementUseCase.suggestPlan(any(), any()))
                .thenThrow(new GroupNotFoundException(GROUP_ID));

        mockMvc.perform(get("/api/groups/{groupId}/settlements", GROUP_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // POST /api/groups/{groupId}/settlements
    // =========================================================================

    @Test
    void record_settlement_returns_201() throws Exception {
        Settlement saved = new Settlement(
                UUID.randomUUID(), GROUP_ID, PAYER_ID, PAYEE_ID,
                new BigDecimal("30.00"), "CONFIRMED",
                OffsetDateTime.now(), OffsetDateTime.now());

        when(settlementUseCase.recordSettlement(eq(CALLER_ID), eq(GROUP_ID), any()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/groups/{groupId}/settlements", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson(PAYER_ID, PAYEE_ID, "30.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.amount").value(30.00));
    }

    @Test
    void record_settlement_invalid_body_returns_400() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/settlements", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payerId":null,"payeeId":null,"amount":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void record_settlement_business_error_returns_400() throws Exception {
        when(settlementUseCase.recordSettlement(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Payer and payee must be different"));

        mockMvc.perform(post("/api/groups/{groupId}/settlements", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson(PAYER_ID, PAYER_ID, "10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payer and payee must be different"));
    }

    private String recordJson(UUID payerId, UUID payeeId, String amount) {
        return """
                {"payerId":"%s","payeeId":"%s","amount":%s}
                """.formatted(payerId, payeeId, amount);
    }
}
