package com.splitit.domain.reminder.service;

import com.splitit.domain.reminder.model.GroupBalanceSnapshot;
import com.splitit.domain.reminder.model.MemberLine;
import com.splitit.domain.reminder.port.in.SendRemindersUseCase;
import com.splitit.domain.reminder.port.out.ReminderDataProvider;
import com.splitit.domain.reminder.port.out.ReminderEmailSender;
import java.math.BigDecimal;
import java.util.List;

/**
 * Pure domain service — no Spring annotations.
 * Sends one reminder email per group per member who owes money (balance < 0).
 */
public class ReminderService implements SendRemindersUseCase {

    private final ReminderDataProvider dataProvider;
    private final ReminderEmailSender emailSender;

    public ReminderService(ReminderDataProvider dataProvider, ReminderEmailSender emailSender) {
        this.dataProvider = dataProvider;
        this.emailSender = emailSender;
    }

    @Override
    public void sendReminders() {
        List<GroupBalanceSnapshot> snapshots = dataProvider.snapshotAllGroups();
        for (GroupBalanceSnapshot snapshot : snapshots) {
            for (MemberLine member : snapshot.members()) {
                if (member.balance().compareTo(BigDecimal.ZERO) < 0) {
                    emailSender.sendBalanceReminder(
                            member.email(),
                            member.displayName(),
                            snapshot.groupName(),
                            member.balance().abs());
                }
            }
        }
    }
}
