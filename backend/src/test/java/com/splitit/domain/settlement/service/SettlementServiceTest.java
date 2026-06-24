package com.splitit.domain.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.settlement.model.MemberNet;
import com.splitit.domain.settlement.model.Settlement;
import com.splitit.domain.settlement.port.in.SettlementUseCase.RecordSettlementCommand;
import com.splitit.domain.settlement.port.out.GroupBalanceProvider;
import com.splitit.domain.settlement.port.out.SettlementRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SettlementServiceTest {

    private SettlementRepository settlementRepository;
    private GroupBalanceProvider balanceProvider;
    private SettlementService service;

    private final UUID GROUP_ID = UUID.randomUUID();
    private final UUID USER_A = UUID.randomUUID();
    private final UUID USER_B = UUID.randomUUID();
    private final UUID USER_C = UUID.randomUUID();
    private final UUID OUTSIDER = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        settlementRepository = mock(SettlementRepository.class);
        balanceProvider = mock(GroupBalanceProvider.class);
        service = new SettlementService(settlementRepository, balanceProvider);

        when(balanceProvider.currentBalances(GROUP_ID)).thenReturn(List.of(
                new MemberNet(USER_A, "Alice", new BigDecimal("-30.00")),
                new MemberNet(USER_B, "Bob", new BigDecimal("-10.00")),
                new MemberNet(USER_C, "Carol", new BigDecimal("40.00"))
        ));

        when(settlementRepository.save(any())).thenAnswer(inv -> {
            Settlement s = inv.getArgument(0);
            return new Settlement(UUID.randomUUID(), s.getGroupId(), s.getPayerId(),
                    s.getPayeeId(), s.getAmount(), s.getStatus(),
                    s.getSettledAt(), OffsetDateTime.now());
        });
    }

    // =========================================================================
    // suggestPlan
    // =========================================================================

    @Nested
    class SuggestPlan {

        @Test
        void returns_non_empty_plan_for_member() {
            var plan = service.suggestPlan(USER_A, GROUP_ID);
            assertThat(plan).isNotEmpty();
        }

        @Test
        void non_member_throws_group_not_found() {
            assertThatThrownBy(() -> service.suggestPlan(OUTSIDER, GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        void empty_group_balances_treated_as_non_existent() {
            when(balanceProvider.currentBalances(GROUP_ID)).thenReturn(List.of());
            assertThatThrownBy(() -> service.suggestPlan(USER_A, GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    // =========================================================================
    // recordSettlement
    // =========================================================================

    @Nested
    class RecordSettlement {

        @Test
        void happy_path_saves_confirmed_settlement() {
            RecordSettlementCommand cmd = new RecordSettlementCommand(
                    USER_A, USER_C, new BigDecimal("30.00"));

            Settlement result = service.recordSettlement(USER_A, GROUP_ID, cmd);

            verify(settlementRepository).save(any());
            assertThat(result.getId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        void non_member_caller_throws_group_not_found() {
            RecordSettlementCommand cmd = new RecordSettlementCommand(
                    USER_A, USER_C, new BigDecimal("10.00"));

            assertThatThrownBy(() -> service.recordSettlement(OUTSIDER, GROUP_ID, cmd))
                    .isInstanceOf(GroupNotFoundException.class);
            verify(settlementRepository, never()).save(any());
        }

        @Test
        void payer_equals_payee_throws_illegal_argument() {
            RecordSettlementCommand cmd = new RecordSettlementCommand(
                    USER_A, USER_A, new BigDecimal("10.00"));

            assertThatThrownBy(() -> service.recordSettlement(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different");
        }

        @Test
        void zero_amount_throws_illegal_argument() {
            RecordSettlementCommand cmd = new RecordSettlementCommand(
                    USER_A, USER_C, BigDecimal.ZERO);

            assertThatThrownBy(() -> service.recordSettlement(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than 0");
        }

        @Test
        void negative_amount_throws_illegal_argument() {
            RecordSettlementCommand cmd = new RecordSettlementCommand(
                    USER_A, USER_C, new BigDecimal("-5.00"));

            assertThatThrownBy(() -> service.recordSettlement(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void payer_not_a_member_throws_illegal_argument() {
            RecordSettlementCommand cmd = new RecordSettlementCommand(
                    OUTSIDER, USER_C, new BigDecimal("10.00"));

            assertThatThrownBy(() -> service.recordSettlement(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a group member");
        }

        @Test
        void payee_not_a_member_throws_illegal_argument() {
            RecordSettlementCommand cmd = new RecordSettlementCommand(
                    USER_A, OUTSIDER, new BigDecimal("10.00"));

            assertThatThrownBy(() -> service.recordSettlement(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a group member");
        }
    }
}
