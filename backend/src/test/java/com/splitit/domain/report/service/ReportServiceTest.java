package com.splitit.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.report.model.GroupReport;
import com.splitit.domain.report.model.ReportBalance;
import com.splitit.domain.report.model.ReportExpense;
import com.splitit.domain.report.model.ReportSettlement;
import com.splitit.domain.report.port.out.ReportDataProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 24);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), ZoneOffset.UTC);

    private ReportDataProvider dataProvider;
    private ReportService service;

    private final UUID GROUP_ID = UUID.randomUUID();
    private final UUID CALLER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dataProvider = mock(ReportDataProvider.class);
        service = new ReportService(dataProvider, FIXED_CLOCK);
    }

    @Test
    void non_member_throws_GroupNotFoundException() {
        when(dataProvider.isGroupMember(GROUP_ID, CALLER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.generate(CALLER_ID, GROUP_ID, null, null))
                .isInstanceOf(GroupNotFoundException.class);
    }

    @Test
    void null_from_defaults_to_epoch() {
        when(dataProvider.isGroupMember(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(dataProvider.findGroupName(GROUP_ID)).thenReturn(Optional.of("Vacation"));
        when(dataProvider.expensesInRange(eq(GROUP_ID), eq(ReportService.EPOCH), eq(FIXED_TODAY)))
                .thenReturn(List.of());
        when(dataProvider.balances(GROUP_ID)).thenReturn(List.of());
        when(dataProvider.settlementPlan(GROUP_ID)).thenReturn(List.of());

        GroupReport report = service.generate(CALLER_ID, GROUP_ID, null, null);

        assertThat(report.from()).isEqualTo(ReportService.EPOCH);
        assertThat(report.to()).isEqualTo(FIXED_TODAY);
    }

    @Test
    void null_to_defaults_to_today_from_clock() {
        when(dataProvider.isGroupMember(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(dataProvider.findGroupName(GROUP_ID)).thenReturn(Optional.of("G"));
        when(dataProvider.expensesInRange(any(), any(), eq(FIXED_TODAY))).thenReturn(List.of());
        when(dataProvider.balances(GROUP_ID)).thenReturn(List.of());
        when(dataProvider.settlementPlan(GROUP_ID)).thenReturn(List.of());

        GroupReport report = service.generate(CALLER_ID, GROUP_ID, LocalDate.of(2026, 1, 1), null);

        assertThat(report.to()).isEqualTo(FIXED_TODAY);
    }

    @Test
    void explicit_dates_are_passed_through() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(dataProvider.isGroupMember(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(dataProvider.findGroupName(GROUP_ID)).thenReturn(Optional.of("G"));
        when(dataProvider.expensesInRange(GROUP_ID, from, to)).thenReturn(List.of());
        when(dataProvider.balances(GROUP_ID)).thenReturn(List.of());
        when(dataProvider.settlementPlan(GROUP_ID)).thenReturn(List.of());

        GroupReport report = service.generate(CALLER_ID, GROUP_ID, from, to);

        assertThat(report.from()).isEqualTo(from);
        assertThat(report.to()).isEqualTo(to);
    }

    @Test
    void report_assembles_data_from_provider() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);

        ReportExpense expense = new ReportExpense(
                LocalDate.of(2026, 3, 10), "Lunch", "Alice", new BigDecimal("60.00"), "EQUAL");
        ReportBalance balance = new ReportBalance("Alice", new BigDecimal("30.00"));
        ReportSettlement settlement = new ReportSettlement("Bob", "Alice", new BigDecimal("30.00"));

        when(dataProvider.isGroupMember(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(dataProvider.findGroupName(GROUP_ID)).thenReturn(Optional.of("Team"));
        when(dataProvider.expensesInRange(GROUP_ID, from, to)).thenReturn(List.of(expense));
        when(dataProvider.balances(GROUP_ID)).thenReturn(List.of(balance));
        when(dataProvider.settlementPlan(GROUP_ID)).thenReturn(List.of(settlement));

        GroupReport report = service.generate(CALLER_ID, GROUP_ID, from, to);

        assertThat(report.groupName()).isEqualTo("Team");
        assertThat(report.expenses()).containsExactly(expense);
        assertThat(report.balances()).containsExactly(balance);
        assertThat(report.settlements()).containsExactly(settlement);
    }

    @Test
    void from_after_to_throws_IllegalArgumentException() {
        LocalDate from = LocalDate.of(2026, 6, 30);
        LocalDate to = LocalDate.of(2026, 1, 1);

        when(dataProvider.isGroupMember(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(dataProvider.findGroupName(GROUP_ID)).thenReturn(Optional.of("G"));

        assertThatThrownBy(() -> service.generate(CALLER_ID, GROUP_ID, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'from' date must not be after 'to' date");
    }

    @Test
    void from_equal_to_is_accepted() {
        LocalDate sameDay = LocalDate.of(2026, 3, 15);

        when(dataProvider.isGroupMember(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(dataProvider.findGroupName(GROUP_ID)).thenReturn(Optional.of("G"));
        when(dataProvider.expensesInRange(GROUP_ID, sameDay, sameDay)).thenReturn(List.of());
        when(dataProvider.balances(GROUP_ID)).thenReturn(List.of());
        when(dataProvider.settlementPlan(GROUP_ID)).thenReturn(List.of());

        GroupReport report = service.generate(CALLER_ID, GROUP_ID, sameDay, sameDay);

        assertThat(report.from()).isEqualTo(sameDay);
        assertThat(report.to()).isEqualTo(sameDay);
    }
}
