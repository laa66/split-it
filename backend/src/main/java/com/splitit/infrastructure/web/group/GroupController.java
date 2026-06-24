package com.splitit.infrastructure.web.group;

import com.splitit.domain.group.model.GroupDetails;
import com.splitit.domain.group.port.in.InviteToGroupUseCase;
import com.splitit.domain.group.port.in.InviteToGroupUseCase.InviteResult;
import com.splitit.domain.group.port.in.ManageGroupsUseCase;
import com.splitit.infrastructure.security.AuthenticatedUser;
import com.splitit.infrastructure.web.group.dto.CreateGroupRequest;
import com.splitit.infrastructure.web.group.dto.GroupDetailsResponse;
import com.splitit.infrastructure.web.group.dto.GroupSummaryResponse;
import com.splitit.infrastructure.web.group.dto.InviteRequest;
import com.splitit.infrastructure.web.group.dto.InviteResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private static final Logger log = LoggerFactory.getLogger(GroupController.class);

    private final ManageGroupsUseCase manageGroupsUseCase;
    private final InviteToGroupUseCase inviteToGroupUseCase;

    public GroupController(ManageGroupsUseCase manageGroupsUseCase,
                           InviteToGroupUseCase inviteToGroupUseCase) {
        this.manageGroupsUseCase = manageGroupsUseCase;
        this.inviteToGroupUseCase = inviteToGroupUseCase;
    }

    @GetMapping
    public List<GroupSummaryResponse> listGroups(@AuthenticationPrincipal AuthenticatedUser user) {
        return manageGroupsUseCase.listGroups(user.id()).stream()
                .map(GroupSummaryResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GroupDetailsResponse createGroup(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody CreateGroupRequest request) {
        GroupDetails details = manageGroupsUseCase.createGroup(user.id(),
                new ManageGroupsUseCase.CreateGroupCommand(request.name(), request.description()));
        log.info("Group created: id={} by user={}", details.group().getId(), user.id());
        return GroupDetailsResponse.from(details);
    }

    @GetMapping("/{id}")
    public GroupDetailsResponse getGroup(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID id) {
        return GroupDetailsResponse.from(manageGroupsUseCase.getGroup(user.id(), id));
    }

    @PostMapping("/{id}/invite")
    @Transactional
    public InviteResponse invite(@AuthenticationPrincipal AuthenticatedUser user,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody InviteRequest request) {
        InviteResult result = inviteToGroupUseCase.invite(user.id(), id, request.email());
        log.info("Invite to group={} by user={} addedImmediately={}",
                id, user.id(), result.addedImmediately());
        String message = result.addedImmediately()
                ? "User was added to the group."
                : "Invitation email sent.";
        return new InviteResponse(result.addedImmediately(), message);
    }
}
