package com.splitit.infrastructure.web.group.dto;

import com.splitit.domain.group.model.Group;
import com.splitit.domain.group.model.GroupDetails;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GroupDetailsResponse(UUID id, String name, String description, UUID createdBy,
                                   OffsetDateTime createdAt, List<MemberResponse> members) {

    public static GroupDetailsResponse from(GroupDetails details) {
        Group g = details.group();
        List<MemberResponse> members = details.members().stream()
                .map(MemberResponse::from)
                .toList();
        return new GroupDetailsResponse(g.getId(), g.getName(), g.getDescription(),
                g.getCreatedBy(), g.getCreatedAt(), members);
    }

    public record MemberResponse(UUID userId, String displayName, String email, String role) {

        public static MemberResponse from(com.splitit.domain.group.model.GroupMember m) {
            return new MemberResponse(m.userId(), m.displayName(), m.email(), m.role().name());
        }
    }
}
