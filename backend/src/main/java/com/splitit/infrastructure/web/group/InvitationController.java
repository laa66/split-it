package com.splitit.infrastructure.web.group;

import com.splitit.domain.group.port.in.AcceptInvitationUseCase;
import com.splitit.infrastructure.web.group.dto.AcceptInvitationResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private static final Logger log = LoggerFactory.getLogger(InvitationController.class);

    private final AcceptInvitationUseCase acceptInvitationUseCase;

    public InvitationController(AcceptInvitationUseCase acceptInvitationUseCase) {
        this.acceptInvitationUseCase = acceptInvitationUseCase;
    }

    /** Public endpoint (token is the credential). Whitelisted in SecurityConfig. */
    @GetMapping("/{token}/accept")
    @Transactional
    public AcceptInvitationResponse accept(@PathVariable UUID token) {
        AcceptInvitationResponse response = AcceptInvitationResponse.from(acceptInvitationUseCase.accept(token));
        log.info("Invitation accepted: token={}", token);
        return response;
    }
}
