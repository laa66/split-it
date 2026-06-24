package com.splitit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitit.infrastructure.email.SpringMailEmailSender;
import com.splitit.infrastructure.web.auth.dto.AuthResponse;
import com.splitit.infrastructure.web.auth.dto.RegisterRequest;
import com.splitit.infrastructure.web.expense.dto.AddExpenseRequest;
import com.splitit.infrastructure.web.expense.dto.AddExpenseRequest.ParticipantShareRequest;
import com.splitit.infrastructure.web.expense.dto.ExpensePageResponse;
import com.splitit.infrastructure.web.expense.dto.ExpenseResponse;
import com.splitit.infrastructure.web.expense.dto.MemberBalanceResponse;
import com.splitit.infrastructure.web.group.dto.CreateGroupRequest;
import com.splitit.infrastructure.web.group.dto.GroupDetailsResponse;
import com.splitit.infrastructure.web.group.dto.InviteRequest;
import com.splitit.infrastructure.web.settlement.dto.RecordSettlementRequest;
import com.splitit.infrastructure.web.settlement.dto.SettlementSuggestionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Full end-to-end integration test against a real PostgreSQL (Testcontainers).
 * Schema is loaded from the shared db/init.sql — exactly as in dev/prod, no Flyway.
 * Email transport is mocked so no SMTP is touched.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SplitItE2EIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../db/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-secret-key-at-least-32-bytes-long");
        // Park the reminder scheduler far away so it never fires mid-test.
        registry.add("app.reminder.cron", () -> "0 0 0 1 1 *");
    }

    @MockBean
    SpringMailEmailSender emailSender;

    @Autowired
    TestRestTemplate rest;

    @Test
    void fullFlow_register_group_invite_expense_balance_settlement() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String aliceEmail = "alice-" + suffix + "@test.com";
        String bobEmail = "bob-" + suffix + "@test.com";
        String carolEmail = "carol-" + suffix + "@test.com";

        String aliceToken = register(aliceEmail, "Alice");
        register(bobEmail, "Bob");
        register(carolEmail, "Carol");

        // Alice creates a group — she is the sole OWNER initially.
        GroupDetailsResponse group = post("/api/groups",
                new CreateGroupRequest("Trip", "Weekend trip"), aliceToken, GroupDetailsResponse.class);
        UUID groupId = group.id();
        assertThat(group.members()).hasSize(1);

        // Both invitees already have accounts → added immediately.
        invite(groupId, bobEmail, aliceToken);
        invite(groupId, carolEmail, aliceToken);

        GroupDetailsResponse withMembers = get("/api/groups/" + groupId, aliceToken, GroupDetailsResponse.class);
        assertThat(withMembers.members()).hasSize(3);
        Map<String, UUID> idByEmail = withMembers.members().stream()
                .collect(Collectors.toMap(GroupDetailsResponse.MemberResponse::email,
                        GroupDetailsResponse.MemberResponse::userId));
        UUID alice = idByEmail.get(aliceEmail);
        UUID bob = idByEmail.get(bobEmail);
        UUID carol = idByEmail.get(carolEmail);

        // Alice pays 90.00, split EQUAL across all three → 30.00 each.
        AddExpenseRequest expense = new AddExpenseRequest(
                "Dinner", new BigDecimal("90.00"), alice,
                com.splitit.domain.expense.model.SplitType.EQUAL, LocalDate.now(),
                List.of(new ParticipantShareRequest(alice, null),
                        new ParticipantShareRequest(bob, null),
                        new ParticipantShareRequest(carol, null)));
        ResponseEntity<ExpenseResponse> created = exchange(
                "/api/groups/" + groupId + "/expenses", HttpMethod.POST, expense, aliceToken, ExpenseResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<UUID, BigDecimal> balances = balances(groupId, aliceToken);
        assertThat(balances.get(alice)).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("60.00"));
        assertThat(balances.get(bob)).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("-30.00"));
        assertThat(balances.get(carol)).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("-30.00"));

        // Settlement plan: two debtors paying Alice, ≤ n-1 transactions.
        SettlementSuggestionResponse[] plan = get(
                "/api/groups/" + groupId + "/settlements", aliceToken, SettlementSuggestionResponse[].class);
        assertThat(plan).hasSize(2);
        assertThat(plan).allSatisfy(s -> {
            assertThat(s.payeeId()).isEqualTo(alice);
            assertThat(s.amount()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("30.00"));
        });

        // Bob pays Alice 30.00 → recorded, immediately reflected in balances and plan.
        ResponseEntity<String> recorded = exchange(
                "/api/groups/" + groupId + "/settlements", HttpMethod.POST,
                new RecordSettlementRequest(bob, alice, new BigDecimal("30.00")), aliceToken, String.class);
        assertThat(recorded.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<UUID, BigDecimal> after = balances(groupId, aliceToken);
        assertThat(after.get(alice)).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("30.00"));
        assertThat(after.get(bob)).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("0.00"));
        assertThat(after.get(carol)).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("-30.00"));

        SettlementSuggestionResponse[] remaining = get(
                "/api/groups/" + groupId + "/settlements", aliceToken, SettlementSuggestionResponse[].class);
        assertThat(remaining).hasSize(1);
        assertThat(remaining[0].payerId()).isEqualTo(carol);
        assertThat(remaining[0].payeeId()).isEqualTo(alice);
    }

    @Test
    void expenseListing_isPaginated() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = register("solo-" + suffix + "@test.com", "Solo");
        GroupDetailsResponse group = post("/api/groups",
                new CreateGroupRequest("Solo", null), token, GroupDetailsResponse.class);
        UUID groupId = group.id();
        UUID userId = group.members().get(0).userId();

        for (int i = 0; i < 3; i++) {
            AddExpenseRequest e = new AddExpenseRequest(
                    "Item " + i, new BigDecimal("10.00"), userId,
                    com.splitit.domain.expense.model.SplitType.EQUAL, LocalDate.now(),
                    List.of(new ParticipantShareRequest(userId, null)));
            exchange("/api/groups/" + groupId + "/expenses", HttpMethod.POST, e, token, ExpenseResponse.class);
        }

        ExpensePageResponse page0 = get(
                "/api/groups/" + groupId + "/expenses?page=0&size=2", token, ExpensePageResponse.class);
        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page0.content()).hasSize(2);

        ExpensePageResponse page1 = get(
                "/api/groups/" + groupId + "/expenses?page=1&size=2", token, ExpensePageResponse.class);
        assertThat(page1.content()).hasSize(1);
        assertThat(page1.page()).isEqualTo(1);
    }

    // --- helpers ---

    private String register(String email, String name) {
        AuthResponse res = post("/api/auth/register",
                new RegisterRequest(email, name, "password123"), null, AuthResponse.class);
        assertThat(res.token()).isNotBlank();
        return res.token();
    }

    private void invite(UUID groupId, String email, String token) {
        ResponseEntity<String> res = exchange(
                "/api/groups/" + groupId + "/invite", HttpMethod.POST, new InviteRequest(email), token, String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private Map<UUID, BigDecimal> balances(UUID groupId, String token) {
        MemberBalanceResponse[] arr = get("/api/groups/" + groupId + "/balance", token, MemberBalanceResponse[].class);
        return List.of(arr).stream()
                .collect(Collectors.toMap(MemberBalanceResponse::userId, MemberBalanceResponse::balance));
    }

    private <T> T post(String url, Object body, String token, Class<T> type) {
        return exchange(url, HttpMethod.POST, body, token, type).getBody();
    }

    private <T> T get(String url, String token, Class<T> type) {
        return exchange(url, HttpMethod.GET, null, token, type).getBody();
    }

    private <T> ResponseEntity<T> exchange(String url, HttpMethod method, Object body, String token, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(url, method, new HttpEntity<>(body, headers), type);
    }
}
