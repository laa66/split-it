package com.splitit.infrastructure.web.settlement;

import com.splitit.domain.settlement.port.in.SettlementUseCase;
import com.splitit.domain.settlement.port.in.SettlementUseCase.RecordSettlementCommand;
import com.splitit.infrastructure.security.AuthenticatedUser;
import com.splitit.infrastructure.web.settlement.dto.RecordSettlementRequest;
import com.splitit.infrastructure.web.settlement.dto.SettlementResponse;
import com.splitit.infrastructure.web.settlement.dto.SettlementSuggestionResponse;
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
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {

    private static final Logger log = LoggerFactory.getLogger(SettlementController.class);

    private final SettlementUseCase settlementUseCase;

    public SettlementController(SettlementUseCase settlementUseCase) {
        this.settlementUseCase = settlementUseCase;
    }

    @GetMapping
    public List<SettlementSuggestionResponse> suggestPlan(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID groupId) {

        return settlementUseCase.suggestPlan(user.id(), groupId).stream()
                .map(SettlementSuggestionResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SettlementResponse recordSettlement(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID groupId,
            @Valid @RequestBody RecordSettlementRequest request) {

        RecordSettlementCommand cmd = new RecordSettlementCommand(
                request.payerId(), request.payeeId(), request.amount());

        SettlementResponse response = SettlementResponse.from(
                settlementUseCase.recordSettlement(user.id(), groupId, cmd));
        log.info("Settlement recorded: group={} payer={} payee={} amount={} by user={}",
                groupId, request.payerId(), request.payeeId(), request.amount(), user.id());
        return response;
    }
}
