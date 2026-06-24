package com.splitit.domain.reminder.port.out;

import com.splitit.domain.reminder.model.GroupBalanceSnapshot;
import java.util.List;

public interface ReminderDataProvider {

    List<GroupBalanceSnapshot> snapshotAllGroups();
}
