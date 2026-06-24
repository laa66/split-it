package com.splitit.domain.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splitit.domain.expense.exception.ExpenseAccessDeniedException;
import com.splitit.domain.expense.exception.ExpenseNotFoundException;
import com.splitit.domain.expense.exception.InvalidSplitException;
import com.splitit.domain.expense.model.Expense;
import com.splitit.domain.expense.model.ExpenseShare;
import com.splitit.domain.expense.model.ExpenseWithShares;
import com.splitit.domain.expense.model.MemberBalance;
import com.splitit.domain.expense.model.SplitType;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase.AddExpenseCommand;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase.ParticipantShare;
import com.splitit.domain.expense.port.out.ExpenseRepository;
import com.splitit.domain.expense.port.out.GroupMembershipPort;
import com.splitit.domain.group.exception.GroupNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExpenseServiceTest {

    private ExpenseRepository expenseRepository;
    private GroupMembershipPort groupMembershipPort;
    private ExpenseService service;

    private final UUID GROUP_ID = UUID.randomUUID();
    private final UUID USER_A = UUID.randomUUID();
    private final UUID USER_B = UUID.randomUUID();
    private final UUID USER_C = UUID.randomUUID();
    private final UUID OWNER_ID = USER_A;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        groupMembershipPort = mock(GroupMembershipPort.class);
        service = new ExpenseService(expenseRepository, groupMembershipPort);

        // Default: all three are members
        when(groupMembershipPort.isMember(eq(GROUP_ID), any())).thenReturn(true);
        when(groupMembershipPort.listMemberIds(GROUP_ID)).thenReturn(List.of(USER_A, USER_B, USER_C));
        when(groupMembershipPort.findGroupOwnerId(GROUP_ID)).thenReturn(OWNER_ID);

        // Default save: return aggregrate back with a generated ID
        when(expenseRepository.save(any(), any())).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            List<ExpenseShare> shares = inv.getArgument(1);
            Expense saved = new Expense(UUID.randomUUID(), e.getGroupId(), e.getPaidBy(),
                    e.getTitle(), e.getAmount(), e.getSplitType(), e.getExpenseDate(),
                    OffsetDateTime.now());
            return new ExpenseWithShares(saved, shares);
        });
    }

    // =========================================================================
    // EQUAL split
    // =========================================================================

    @Nested
    class EqualSplit {

        @Test
        void equal_90_dividedBy_3_gives_30_each() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, null),
                    new ParticipantShare(USER_B, null),
                    new ParticipantShare(USER_C, null));

            AddExpenseCommand cmd = cmd("Dinner", "90.00", USER_A, SplitType.EQUAL, participants);
            ExpenseWithShares result = service.addExpense(USER_A, GROUP_ID, cmd);

            assertShares(result, "30.00", "30.00", "30.00");
            assertSumEqualsAmount(result, new BigDecimal("90.00"));
        }

        @Test
        void equal_10_dividedBy_3_gives_3_34_3_33_3_33() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, null),
                    new ParticipantShare(USER_B, null),
                    new ParticipantShare(USER_C, null));

            AddExpenseCommand cmd = cmd("Lunch", "10.00", USER_A, SplitType.EQUAL, participants);
            ExpenseWithShares result = service.addExpense(USER_A, GROUP_ID, cmd);

            assertShares(result, "3.34", "3.33", "3.33");
            assertSumEqualsAmount(result, new BigDecimal("10.00"));
        }

        @Test
        void equal_100_dividedBy_3_gives_33_34_33_33_33_33() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, null),
                    new ParticipantShare(USER_B, null),
                    new ParticipantShare(USER_C, null));

            AddExpenseCommand cmd = cmd("Hotel", "100.00", USER_A, SplitType.EQUAL, participants);
            ExpenseWithShares result = service.addExpense(USER_A, GROUP_ID, cmd);

            assertShares(result, "33.34", "33.33", "33.33");
            assertSumEqualsAmount(result, new BigDecimal("100.00"));
        }

        @Test
        void equal_0_01_dividedBy_2_gives_0_01_and_0_00() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, null),
                    new ParticipantShare(USER_B, null));

            AddExpenseCommand cmd = cmd("Cent", "0.01", USER_A, SplitType.EQUAL, participants);
            ExpenseWithShares result = service.addExpense(USER_A, GROUP_ID, cmd);

            assertShares(result, "0.01", "0.00");
            assertSumEqualsAmount(result, new BigDecimal("0.01"));
        }
    }

    // =========================================================================
    // PERCENTAGE split
    // =========================================================================

    @Nested
    class PercentageSplit {

        @Test
        void percentage_50_50_on_100() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, new BigDecimal("50.00")),
                    new ParticipantShare(USER_B, new BigDecimal("50.00")));

            AddExpenseCommand cmd = cmd("Split", "100.00", USER_A, SplitType.PERCENTAGE, participants);
            ExpenseWithShares result = service.addExpense(USER_A, GROUP_ID, cmd);

            assertShares(result, "50.00", "50.00");
            assertSumEqualsAmount(result, new BigDecimal("100.00"));
        }

        @Test
        void percentage_33_33_33_34_on_10_handles_penny_remainder() {
            // 33.33 + 33.33 + 33.34 = 100.00
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, new BigDecimal("33.33")),
                    new ParticipantShare(USER_B, new BigDecimal("33.33")),
                    new ParticipantShare(USER_C, new BigDecimal("33.34")));

            AddExpenseCommand cmd = cmd("Share", "10.00", USER_A, SplitType.PERCENTAGE, participants);
            ExpenseWithShares result = service.addExpense(USER_A, GROUP_ID, cmd);

            assertSumEqualsAmount(result, new BigDecimal("10.00"));
        }

        @Test
        void percentage_not_summing_to_100_throws() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, new BigDecimal("40.00")),
                    new ParticipantShare(USER_B, new BigDecimal("40.00")));

            AddExpenseCommand cmd = cmd("Bad", "100.00", USER_A, SplitType.PERCENTAGE, participants);

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(InvalidSplitException.class)
                    .hasMessageContaining("100.00");
        }
    }

    // =========================================================================
    // AMOUNT split
    // =========================================================================

    @Nested
    class AmountSplit {

        @Test
        void amount_exact_sum_passes() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, new BigDecimal("60.00")),
                    new ParticipantShare(USER_B, new BigDecimal("30.00")));

            AddExpenseCommand cmd = cmd("Rent", "90.00", USER_A, SplitType.AMOUNT, participants);
            ExpenseWithShares result = service.addExpense(USER_A, GROUP_ID, cmd);

            assertShares(result, "60.00", "30.00");
            assertSumEqualsAmount(result, new BigDecimal("90.00"));
        }

        @Test
        void amount_not_matching_total_throws() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, new BigDecimal("40.00")),
                    new ParticipantShare(USER_B, new BigDecimal("40.00")));

            AddExpenseCommand cmd = cmd("Bad", "90.00", USER_A, SplitType.AMOUNT, participants);

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(InvalidSplitException.class);
        }
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @Nested
    class Validation {

        @Test
        void blank_title_throws() {
            AddExpenseCommand cmd = cmd("", "10.00", USER_A, SplitType.EQUAL,
                    List.of(new ParticipantShare(USER_A, null)));

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Title");
        }

        @Test
        void zero_amount_throws() {
            AddExpenseCommand cmd = cmd("Test", "0.00", USER_A, SplitType.EQUAL,
                    List.of(new ParticipantShare(USER_A, null)));

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount");
        }

        @Test
        void negative_amount_throws() {
            AddExpenseCommand cmd = cmd("Test", "-1.00", USER_A, SplitType.EQUAL,
                    List.of(new ParticipantShare(USER_A, null)));

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount");
        }

        @Test
        void empty_participants_throws() {
            AddExpenseCommand cmd = cmd("Test", "10.00", USER_A, SplitType.EQUAL, List.of());

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("participant");
        }

        @Test
        void duplicate_participant_throws() {
            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, null),
                    new ParticipantShare(USER_A, null));

            AddExpenseCommand cmd = cmd("Test", "10.00", USER_A, SplitType.EQUAL, participants);

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicate");
        }

        @Test
        void non_member_paidBy_throws_404() {
            UUID outsider = UUID.randomUUID();
            when(groupMembershipPort.isMember(GROUP_ID, outsider)).thenReturn(false);

            List<ParticipantShare> participants = List.of(new ParticipantShare(USER_A, null));
            AddExpenseCommand cmd = cmd("Test", "10.00", outsider, SplitType.EQUAL, participants);

            // caller is member but paidBy is not — should throw GroupNotFoundException
            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        void non_member_participant_throws_400() {
            UUID outsider = UUID.randomUUID();
            // outsider is not in listMemberIds
            when(groupMembershipPort.listMemberIds(GROUP_ID)).thenReturn(List.of(USER_A, USER_B));

            List<ParticipantShare> participants = List.of(
                    new ParticipantShare(USER_A, null),
                    new ParticipantShare(outsider, null));

            AddExpenseCommand cmd = cmd("Test", "10.00", USER_A, SplitType.EQUAL, participants);

            assertThatThrownBy(() -> service.addExpense(USER_A, GROUP_ID, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a group member");
        }
    }

    // =========================================================================
    // Authorization
    // =========================================================================

    @Nested
    class Authorization {

        @Test
        void non_member_caller_addExpense_throws_404() {
            UUID outsider = UUID.randomUUID();
            when(groupMembershipPort.isMember(GROUP_ID, outsider)).thenReturn(false);

            AddExpenseCommand cmd = cmd("Test", "10.00", USER_A, SplitType.EQUAL,
                    List.of(new ParticipantShare(USER_A, null)));

            assertThatThrownBy(() -> service.addExpense(outsider, GROUP_ID, cmd))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        void non_member_caller_listExpenses_throws_404() {
            UUID outsider = UUID.randomUUID();
            when(groupMembershipPort.isMember(GROUP_ID, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.listExpenses(outsider, GROUP_ID, 0, 20))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        void non_member_caller_balance_throws_404() {
            UUID outsider = UUID.randomUUID();
            when(groupMembershipPort.isMember(GROUP_ID, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.balance(outsider, GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        void delete_by_non_member_throws_404() {
            UUID expenseId = UUID.randomUUID();
            UUID outsider = UUID.randomUUID();

            Expense expense = new Expense(expenseId, GROUP_ID, USER_A, "Dinner",
                    new BigDecimal("90.00"), SplitType.EQUAL, LocalDate.now(), OffsetDateTime.now());
            when(expenseRepository.findById(expenseId))
                    .thenReturn(Optional.of(new ExpenseWithShares(expense, List.of())));
            when(groupMembershipPort.isMember(GROUP_ID, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteExpense(outsider, expenseId))
                    .isInstanceOf(ExpenseNotFoundException.class);
        }

        @Test
        void delete_by_non_creator_non_owner_throws_403() {
            UUID expenseId = UUID.randomUUID();

            Expense expense = new Expense(expenseId, GROUP_ID, USER_A, "Dinner",
                    new BigDecimal("90.00"), SplitType.EQUAL, LocalDate.now(), OffsetDateTime.now());
            when(expenseRepository.findById(expenseId))
                    .thenReturn(Optional.of(new ExpenseWithShares(expense, List.of())));
            // USER_B is member but not creator and not owner
            when(groupMembershipPort.findGroupOwnerId(GROUP_ID)).thenReturn(USER_A);

            assertThatThrownBy(() -> service.deleteExpense(USER_B, expenseId))
                    .isInstanceOf(ExpenseAccessDeniedException.class);
            verify(expenseRepository, never()).delete(any());
        }

        @Test
        void delete_by_creator_succeeds() {
            UUID expenseId = UUID.randomUUID();

            Expense expense = new Expense(expenseId, GROUP_ID, USER_A, "Dinner",
                    new BigDecimal("90.00"), SplitType.EQUAL, LocalDate.now(), OffsetDateTime.now());
            when(expenseRepository.findById(expenseId))
                    .thenReturn(Optional.of(new ExpenseWithShares(expense, List.of())));
            when(groupMembershipPort.findGroupOwnerId(GROUP_ID)).thenReturn(USER_B);

            service.deleteExpense(USER_A, expenseId); // USER_A is creator
            verify(expenseRepository).delete(expenseId);
        }

        @Test
        void delete_by_group_owner_succeeds() {
            UUID expenseId = UUID.randomUUID();

            Expense expense = new Expense(expenseId, GROUP_ID, USER_B, "Dinner",
                    new BigDecimal("90.00"), SplitType.EQUAL, LocalDate.now(), OffsetDateTime.now());
            when(expenseRepository.findById(expenseId))
                    .thenReturn(Optional.of(new ExpenseWithShares(expense, List.of())));
            when(groupMembershipPort.findGroupOwnerId(GROUP_ID)).thenReturn(USER_A);

            service.deleteExpense(USER_A, expenseId); // USER_A is owner
            verify(expenseRepository).delete(expenseId);
        }

        @Test
        void delete_nonexistent_expense_throws_404() {
            UUID expenseId = UUID.randomUUID();
            when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteExpense(USER_A, expenseId))
                    .isInstanceOf(ExpenseNotFoundException.class);
        }
    }

    // =========================================================================
    // Balance
    // =========================================================================

    @Nested
    class Balance {

        @Test
        void balance_90_equal_3_persons_payer_plus60_others_minus30() {
            // A paid 90, split EQUAL in 3 → A: 90-30=+60, B: 0-30=-30, C: 0-30=-30
            List<MemberBalance> balances = List.of(
                    new MemberBalance(USER_A, "Alice", new BigDecimal("60.00")),
                    new MemberBalance(USER_B, "Bob", new BigDecimal("-30.00")),
                    new MemberBalance(USER_C, "Carol", new BigDecimal("-30.00")));

            when(expenseRepository.calculateBalances(GROUP_ID)).thenReturn(balances);

            List<MemberBalance> result = service.balance(USER_A, GROUP_ID);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getBalance()).isEqualByComparingTo("60.00");
            assertThat(result.get(1).getBalance()).isEqualByComparingTo("-30.00");
            assertThat(result.get(2).getBalance()).isEqualByComparingTo("-30.00");
        }

        @Test
        void balance_sum_is_zero() {
            List<MemberBalance> balances = List.of(
                    new MemberBalance(USER_A, "Alice", new BigDecimal("60.00")),
                    new MemberBalance(USER_B, "Bob", new BigDecimal("-30.00")),
                    new MemberBalance(USER_C, "Carol", new BigDecimal("-30.00")));

            when(expenseRepository.calculateBalances(GROUP_ID)).thenReturn(balances);

            List<MemberBalance> result = service.balance(USER_A, GROUP_ID);

            BigDecimal sum = result.stream()
                    .map(MemberBalance::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum.compareTo(BigDecimal.ZERO)).isEqualTo(0);
        }

        @Test
        void balance_with_confirmed_settlement_adjusts_correctly() {
            // Settlement: B paid A 30 (CONFIRMED) → B's debt reduced, A's surplus reduced
            // A: 60 - 30 = 30, B: -30 + 30 = 0, C: -30
            List<MemberBalance> balancesWithSettlement = List.of(
                    new MemberBalance(USER_A, "Alice", new BigDecimal("30.00")),
                    new MemberBalance(USER_B, "Bob", new BigDecimal("0.00")),
                    new MemberBalance(USER_C, "Carol", new BigDecimal("-30.00")));

            when(expenseRepository.calculateBalances(GROUP_ID)).thenReturn(balancesWithSettlement);

            List<MemberBalance> result = service.balance(USER_A, GROUP_ID);

            BigDecimal sum = result.stream()
                    .map(MemberBalance::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum.compareTo(BigDecimal.ZERO)).isEqualTo(0);
            assertThat(result.get(1).getBalance()).isEqualByComparingTo("0.00");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AddExpenseCommand cmd(String title, String amount, UUID paidBy,
                                  SplitType splitType, List<ParticipantShare> participants) {
        return new AddExpenseCommand(title, new BigDecimal(amount), paidBy, splitType,
                LocalDate.of(2024, 1, 1), participants);
    }

    private void assertShares(ExpenseWithShares result, String... expectedAmounts) {
        List<ExpenseShare> shares = result.getShares();
        assertThat(shares).hasSize(expectedAmounts.length);
        for (int i = 0; i < expectedAmounts.length; i++) {
            assertThat(shares.get(i).getShareAmount())
                    .as("Share[%d]", i)
                    .isEqualByComparingTo(expectedAmounts[i]);
        }
    }

    private void assertSumEqualsAmount(ExpenseWithShares result, BigDecimal expectedTotal) {
        BigDecimal sum = result.getShares().stream()
                .map(ExpenseShare::getShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(expectedTotal);
    }
}
