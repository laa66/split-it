package com.splitit.infrastructure.web.group.dto;

import com.splitit.domain.group.port.in.AcceptInvitationUseCase.AcceptResult;
import java.util.UUID;

public record AcceptInvitationResponse(UUID groupId, String groupName, boolean requiresRegistration,
                                       String message) {

    public static AcceptInvitationResponse from(AcceptResult result) {
        String message = result.requiresRegistration()
                ? "Please register with " + result.invitedEmail() + " to join the group."
                : "You have joined the group.";
        return new AcceptInvitationResponse(result.groupId(), result.groupName(),
                result.requiresRegistration(), message);
    }
}
