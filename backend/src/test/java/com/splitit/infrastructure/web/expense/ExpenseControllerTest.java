package com.splitit.infrastructure.web.expense;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitit.domain.expense.exception.ExpenseAccessDeniedException;
import com.splitit.domain.expense.exception.ExpenseNotFoundException;
import com.splitit.domain.expense.model.Expense;
import com.splitit.domain.expense.model.ExpenseShare;
import com.splitit.domain.expense.model.ExpenseWithShares;
import com.splitit.domain.expense.model.MemberBalance;
import com.splitit.domain.expense.model.SplitType;
import com.splitit.domain.expense.port.in.CalculateBalanceUseCase;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase.ExpensePage;
import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.infrastructure.security.AuthenticatedUser;
import com.splitit.infrastructure.security.JwtTokenProvider;
import com.splitit.infrastructure.web.shared.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(controllers = {ExpenseController.class, StandaloneExpenseController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManageExpensesUseCase manageExpensesUseCase;

    @MockBean
    private CalculateBalanceUseCase calculateBalanceUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID GROUP_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();
    private final UUID EXPENSE_ID = UUID.randomUUID();

    @BeforeEach
    void setUpPrincipal() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "alice@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    // =========================================================================
    // POST /api/groups/{groupId}/expenses
    // =========================================================================

    @Test
    void addExpense_happy_path_returns_201() throws Exception {
        ExpenseWithShares ews = sampleExpenseWithShares();
        when(manageExpensesUseCase.addExpense(eq(USER_ID), eq(GROUP_ID), any())).thenReturn(ews);

        mockMvc.perform(post("/api/groups/{groupId}/expenses", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addExpenseJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.title").value("Dinner"))
                .andExpect(jsonPath("$.shares").isArray());
    }

    @Test
    void addExpense_not_member_returns_404() throws Exception {
        when(manageExpensesUseCase.addExpense(any(), any(), any()))
                .thenThrow(new GroupNotFoundException(GROUP_ID));

        mockMvc.perform(post("/api/groups/{groupId}/expenses", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addExpenseJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void addExpense_invalid_body_returns_400() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/expenses", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","amount":-1,"paidBy":null,"splitType":"EQUAL",
                                 "expenseDate":"2024-01-01","participants":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void addExpense_no_principal_throws_npe_mapped_to_500() throws Exception {
        // Filters are disabled in @WebMvcTest — JWT enforcement is covered by SecurityConfig
        // integration. Here we verify the controller handles a missing principal gracefully:
        // with addFilters=false the request reaches the controller with null principal.
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/api/groups/{groupId}/expenses", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addExpenseJson()))
                // Without filters, Spring MVC still tries to resolve @AuthenticationPrincipal
                // as null, which causes a NullPointerException — mapped by GlobalExceptionHandler
                // to 500. Real 401 is enforced by JwtAuthenticationFilter (SecurityConfig),
                // not tested here.
                .andExpect(status().isInternalServerError());
    }

    // =========================================================================
    // GET /api/groups/{groupId}/expenses
    // =========================================================================

    @Test
    void listExpenses_happy_path_returns_200() throws Exception {
        ExpenseWithShares ews = sampleExpenseWithShares();
        ExpensePage page = new ExpensePage(List.of(ews), 0, 20, 1L, 1);
        when(manageExpensesUseCase.listExpenses(eq(USER_ID), eq(GROUP_ID), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/groups/{groupId}/expenses", GROUP_ID)
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listExpenses_not_member_returns_404() throws Exception {
        when(manageExpensesUseCase.listExpenses(any(), any(), any(Integer.class), any(Integer.class)))
                .thenThrow(new GroupNotFoundException(GROUP_ID));

        mockMvc.perform(get("/api/groups/{groupId}/expenses", GROUP_ID))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // DELETE /api/expenses/{id}
    // =========================================================================

    @Test
    void deleteExpense_happy_path_returns_204() throws Exception {
        doNothing().when(manageExpensesUseCase).deleteExpense(USER_ID, EXPENSE_ID);

        mockMvc.perform(delete("/api/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteExpense_not_found_returns_404() throws Exception {
        doThrow(new ExpenseNotFoundException(EXPENSE_ID))
                .when(manageExpensesUseCase).deleteExpense(eq(USER_ID), eq(EXPENSE_ID));

        mockMvc.perform(delete("/api/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteExpense_forbidden_returns_403() throws Exception {
        doThrow(new ExpenseAccessDeniedException())
                .when(manageExpensesUseCase).deleteExpense(eq(USER_ID), eq(EXPENSE_ID));

        mockMvc.perform(delete("/api/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // GET /api/groups/{groupId}/balance
    // =========================================================================

    @Test
    void balance_happy_path_returns_200() throws Exception {
        List<MemberBalance> balances = List.of(
                new MemberBalance(USER_ID, "Alice", new BigDecimal("60.00")),
                new MemberBalance(UUID.randomUUID(), "Bob", new BigDecimal("-60.00")));
        when(calculateBalanceUseCase.balance(eq(USER_ID), eq(GROUP_ID))).thenReturn(balances);

        mockMvc.perform(get("/api/groups/{groupId}/balance", GROUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Alice"))
                .andExpect(jsonPath("$[0].balance").value(60.00))
                .andExpect(jsonPath("$[1].balance").value(-60.00));
    }

    @Test
    void balance_not_member_returns_404() throws Exception {
        when(calculateBalanceUseCase.balance(any(), any()))
                .thenThrow(new GroupNotFoundException(GROUP_ID));

        mockMvc.perform(get("/api/groups/{groupId}/balance", GROUP_ID))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ExpenseWithShares sampleExpenseWithShares() {
        UUID paidBy = USER_ID;
        Expense e = new Expense(EXPENSE_ID, GROUP_ID, paidBy, "Dinner",
                new BigDecimal("90.00"), SplitType.EQUAL, LocalDate.of(2024, 1, 1),
                OffsetDateTime.now());
        List<ExpenseShare> shares = List.of(
                new ExpenseShare(paidBy, new BigDecimal("30.00")),
                new ExpenseShare(UUID.randomUUID(), new BigDecimal("30.00")),
                new ExpenseShare(UUID.randomUUID(), new BigDecimal("30.00")));
        return new ExpenseWithShares(e, shares);
    }

    private String addExpenseJson() {
        return """
                {
                  "title": "Dinner",
                  "amount": 90.00,
                  "paidBy": "%s",
                  "splitType": "EQUAL",
                  "expenseDate": "2024-01-01",
                  "participants": [
                    {"userId": "%s"},
                    {"userId": "%s"},
                    {"userId": "%s"}
                  ]
                }""".formatted(USER_ID, USER_ID, UUID.randomUUID(), UUID.randomUUID());
    }
}
