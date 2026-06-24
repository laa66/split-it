package com.splitit.domain.reminder.port.out;

import java.math.BigDecimal;

public interface ReminderEmailSender {

    void sendBalanceReminder(String toEmail, String displayName, String groupName, BigDecimal amountOwed);
}
