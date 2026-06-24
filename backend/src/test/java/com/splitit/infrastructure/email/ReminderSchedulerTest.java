package com.splitit.infrastructure.email;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.splitit.domain.reminder.port.in.SendRemindersUseCase;
import org.junit.jupiter.api.Test;

class ReminderSchedulerTest {

    @Test
    void delegates_to_use_case() {
        SendRemindersUseCase useCase = mock(SendRemindersUseCase.class);
        ReminderScheduler scheduler = new ReminderScheduler(useCase);

        scheduler.sendReminders();

        verify(useCase).sendReminders();
    }

    @Test
    void swallows_exception_from_use_case() {
        SendRemindersUseCase useCase = mock(SendRemindersUseCase.class);
        doThrow(new RuntimeException("DB down")).when(useCase).sendReminders();
        ReminderScheduler scheduler = new ReminderScheduler(useCase);

        // must not propagate
        scheduler.sendReminders();
    }
}
