package com.splitit.domain.reminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splitit.domain.reminder.model.GroupBalanceSnapshot;
import com.splitit.domain.reminder.model.MemberLine;
import com.splitit.domain.reminder.port.out.ReminderDataProvider;
import com.splitit.domain.reminder.port.out.ReminderEmailSender;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReminderServiceTest {

    private ReminderDataProvider dataProvider;
    private ReminderEmailSender emailSender;
    private ReminderService service;

    @BeforeEach
    void setUp() {
        dataProvider = mock(ReminderDataProvider.class);
        emailSender = mock(ReminderEmailSender.class);
        service = new ReminderService(dataProvider, emailSender);
    }

    @Test
    void empty_group_list_sends_no_emails() {
        when(dataProvider.snapshotAllGroups()).thenReturn(List.of());

        service.sendReminders();

        verify(emailSender, never()).sendBalanceReminder(any(), any(), any(), any());
    }

    @Test
    void member_with_positive_balance_does_not_receive_email() {
        MemberLine creditor = new MemberLine("alice@example.com", "Alice", new BigDecimal("50.00"));
        GroupBalanceSnapshot snapshot = new GroupBalanceSnapshot(
                UUID.randomUUID(), "Vacation", List.of(creditor));
        when(dataProvider.snapshotAllGroups()).thenReturn(List.of(snapshot));

        service.sendReminders();

        verify(emailSender, never()).sendBalanceReminder(any(), any(), any(), any());
    }

    @Test
    void member_with_zero_balance_does_not_receive_email() {
        MemberLine even = new MemberLine("bob@example.com", "Bob", BigDecimal.ZERO);
        GroupBalanceSnapshot snapshot = new GroupBalanceSnapshot(
                UUID.randomUUID(), "Office", List.of(even));
        when(dataProvider.snapshotAllGroups()).thenReturn(List.of(snapshot));

        service.sendReminders();

        verify(emailSender, never()).sendBalanceReminder(any(), any(), any(), any());
    }

    @Test
    void member_with_negative_balance_receives_email_with_absolute_amount() {
        MemberLine debtor = new MemberLine("carol@example.com", "Carol", new BigDecimal("-30.00"));
        GroupBalanceSnapshot snapshot = new GroupBalanceSnapshot(
                UUID.randomUUID(), "Trip", List.of(debtor));
        when(dataProvider.snapshotAllGroups()).thenReturn(List.of(snapshot));

        service.sendReminders();

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> groupCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(emailSender).sendBalanceReminder(
                toCaptor.capture(), nameCaptor.capture(), groupCaptor.capture(), amountCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("carol@example.com");
        assertThat(nameCaptor.getValue()).isEqualTo("Carol");
        assertThat(groupCaptor.getValue()).isEqualTo("Trip");
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void only_debtors_in_mixed_group_receive_emails() {
        MemberLine debtor = new MemberLine("dave@example.com", "Dave", new BigDecimal("-15.50"));
        MemberLine creditor = new MemberLine("eve@example.com", "Eve", new BigDecimal("15.50"));
        GroupBalanceSnapshot snapshot = new GroupBalanceSnapshot(
                UUID.randomUUID(), "Dinner", List.of(debtor, creditor));
        when(dataProvider.snapshotAllGroups()).thenReturn(List.of(snapshot));

        service.sendReminders();

        verify(emailSender, times(1)).sendBalanceReminder(any(), any(), any(), any());
        verify(emailSender).sendBalanceReminder(
                "dave@example.com", "Dave", "Dinner", new BigDecimal("15.50"));
    }

    @Test
    void multiple_groups_each_debtor_gets_separate_email() {
        MemberLine debtor1 = new MemberLine("a@x.com", "A", new BigDecimal("-10.00"));
        MemberLine debtor2 = new MemberLine("b@x.com", "B", new BigDecimal("-20.00"));
        GroupBalanceSnapshot group1 = new GroupBalanceSnapshot(UUID.randomUUID(), "G1", List.of(debtor1));
        GroupBalanceSnapshot group2 = new GroupBalanceSnapshot(UUID.randomUUID(), "G2", List.of(debtor2));
        when(dataProvider.snapshotAllGroups()).thenReturn(List.of(group1, group2));

        service.sendReminders();

        verify(emailSender, times(2)).sendBalanceReminder(any(), any(), any(), any());
        verify(emailSender).sendBalanceReminder("a@x.com", "A", "G1", new BigDecimal("10.00"));
        verify(emailSender).sendBalanceReminder("b@x.com", "B", "G2", new BigDecimal("20.00"));
    }

    @Test
    void same_member_in_two_groups_gets_two_emails() {
        MemberLine m1 = new MemberLine("alice@x.com", "Alice", new BigDecimal("-5.00"));
        MemberLine m2 = new MemberLine("alice@x.com", "Alice", new BigDecimal("-8.00"));
        GroupBalanceSnapshot g1 = new GroupBalanceSnapshot(UUID.randomUUID(), "GroupA", List.of(m1));
        GroupBalanceSnapshot g2 = new GroupBalanceSnapshot(UUID.randomUUID(), "GroupB", List.of(m2));
        when(dataProvider.snapshotAllGroups()).thenReturn(List.of(g1, g2));

        service.sendReminders();

        verify(emailSender, times(2)).sendBalanceReminder(any(), any(), any(), any());
    }
}
