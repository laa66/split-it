package com.splitit.domain.reminder.model;

import java.util.List;
import java.util.UUID;

public record GroupBalanceSnapshot(UUID groupId, String groupName, List<MemberLine> members) {
}
