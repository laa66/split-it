package com.splitit.infrastructure.email;

import com.splitit.domain.reminder.port.in.SendRemindersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final SendRemindersUseCase sendRemindersUseCase;
    private final String cron;

    public ReminderScheduler(SendRemindersUseCase sendRemindersUseCase,
                             @Value("${app.reminder.cron}") String cron) {
        this.sendRemindersUseCase = sendRemindersUseCase;
        this.cron = cron;
    }

    @Scheduled(cron = "${app.reminder.cron}")
    public void sendReminders() {
        log.info("Reminder job firing (cron='{}')", cron);
        long start = System.currentTimeMillis();
        try {
            sendRemindersUseCase.sendReminders();
            log.info("Reminder job finished in {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Reminder job failed", e);
        }
    }
}
