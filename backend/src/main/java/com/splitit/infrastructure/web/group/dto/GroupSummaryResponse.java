package com.splitit.infrastructure.web.group.dto;

import com.splitit.domain.group.model.GroupSummary;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GroupSummaryResponse(UUID id, String name, String description, String role,
                                   int membersCount, OffsetDateTime createdAt) {

    public static GroupSummaryResponse from(GroupSummary s) {
        return new GroupSummaryResponse(s.id(), s.name(), s.description(),
                s.currentUserRole().name(), s.membersCount(), s.createdAt());
    }
}
