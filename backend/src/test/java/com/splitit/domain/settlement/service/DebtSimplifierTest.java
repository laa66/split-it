package com.splitit.domain.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitit.domain.settlement.model.MemberNet;
import com.splitit.domain.settlement.model.SettlementSuggestion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DebtSimplifierTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();
    private static final UUID D = UUID.randomUUID();
    private static final UUID E = UUID.randomUUID();

    @Test
    void two_members_A_owes_B() {
        // A: -30, B: +30
        List<MemberNet> balances = List.of(
                net(A, "Alice", "-30.00"),
                net(B, "Bob", "30.00"));

        List<SettlementSuggestion> plan = DebtSimplifier.simplify(balances);

        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).getPayerId()).isEqualTo(A);
        assertThat(plan.get(0).getPayeeId()).isEqualTo(B);
        assertThat(plan.get(0).getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void three_members_at_most_two_transactions() {
        // A: -20, B: -10, C: +30  → max 2 transactions
        List<MemberNet> balances = List.of(
                net(A, "Alice", "-20.00"),
                net(B, "Bob", "-10.00"),
                net(C, "Carol", "30.00"));

        List<SettlementSuggestion> plan = DebtSimplifier.simplify(balances);

        assertThat(plan.size()).isLessThanOrEqualTo(2);
        BigDecimal totalTransferred = plan.stream()
                .map(SettlementSuggestion::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo("30.00");
    }

    @Test
    void five_members_at_most_n_minus_one_transactions() {
        // debts: A=-50, B=-30, C=-20; credits: D=+60, E=+40
        List<MemberNet> balances = List.of(
                net(A, "Alice", "-50.00"),
                net(B, "Bob", "-30.00"),
                net(C, "Carol", "-20.00"),
                net(D, "Dave", "60.00"),
                net(E, "Eve", "40.00"));

        List<SettlementSuggestion> plan = DebtSimplifier.simplify(balances);

        assertThat(plan.size()).isLessThanOrEqualTo(4); // n-1 = 4
        BigDecimal totalDebts = new BigDecimal("100.00");
        BigDecimal totalTransferred = plan.stream()
                .map(SettlementSuggestion::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo(totalDebts);
    }

    @Test
    void all_zero_balances_returns_empty_plan() {
        List<MemberNet> balances = List.of(
                net(A, "Alice", "0.00"),
                net(B, "Bob", "0.00"),
                net(C, "Carol", "0.00"));

        List<SettlementSuggestion> plan = DebtSimplifier.simplify(balances);

        assertThat(plan).isEmpty();
    }

    @Test
    void penny_precision_sum_of_transfers_equals_sum_of_debts() {
        // Amounts with cents; A: -33.33, B: -33.33, C: +66.66
        List<MemberNet> balances = List.of(
                net(A, "Alice", "-33.33"),
                net(B, "Bob", "-33.33"),
                net(C, "Carol", "66.66"));

        List<SettlementSuggestion> plan = DebtSimplifier.simplify(balances);

        BigDecimal totalTransferred = plan.stream()
                .map(SettlementSuggestion::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo("66.66");
    }

    @Test
    void near_zero_balances_are_ignored() {
        // B has effectively zero balance (sub-cent), should not appear in plan
        List<MemberNet> balances = List.of(
                net(A, "Alice", "-10.00"),
                net(B, "Bob", "0.00"),
                net(C, "Carol", "10.00"));

        List<SettlementSuggestion> plan = DebtSimplifier.simplify(balances);

        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).getPayerId()).isEqualTo(A);
        assertThat(plan.get(0).getPayeeId()).isEqualTo(C);
    }

    @Test
    void one_cent_residual_is_not_dropped() {
        // A owes 0.02, B and C are each owed 0.01.
        // Bug: with <= CENT_THRESHOLD in loop, the 0.01 residual on A after first transfer was lost.
        List<MemberNet> balances = List.of(
                net(A, "Alice", "-0.02"),
                net(B, "Bob", "0.01"),
                net(C, "Carol", "0.01"));

        List<SettlementSuggestion> plan = DebtSimplifier.simplify(balances);

        BigDecimal totalTransferred = plan.stream()
                .map(SettlementSuggestion::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo("0.02");
    }

    private static MemberNet net(UUID id, String name, String balance) {
        return new MemberNet(id, name, new BigDecimal(balance));
    }
}
