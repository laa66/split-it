package com.splitit.domain.settlement.service;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.settlement.model.MemberNet;
import com.splitit.domain.settlement.model.Settlement;
import com.splitit.domain.settlement.model.SettlementSuggestion;
import com.splitit.domain.settlement.port.in.SettlementUseCase;
import com.splitit.domain.settlement.port.out.GroupBalanceProvider;
import com.splitit.domain.settlement.port.out.SettlementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pure domain service — no Spring annotations.
 * Authorization enforced here: non-member → GroupNotFoundException (no group existence leak).
 */
public class SettlementService implements SettlementUseCase {

    private final SettlementRepository settlementRepository;
    private final GroupBalanceProvider balanceProvider;

    public SettlementService(SettlementRepository settlementRepository,
                             GroupBalanceProvider balanceProvider) {
        this.settlementRepository = settlementRepository;
        this.balanceProvider = balanceProvider;
    }

    @Override
    public List<SettlementSuggestion> suggestPlan(UUID callerId, UUID groupId) {
        List<MemberNet> balances = balanceProvider.currentBalances(groupId);
        requireMember(groupId, callerId, balances);
        return DebtSimplifier.simplify(balances);
    }

    @Override
    public Settlement recordSettlement(UUID callerId, UUID groupId, RecordSettlementCommand cmd) {
        List<MemberNet> balances = balanceProvider.currentBalances(groupId);
        requireMember(groupId, callerId, balances);

        Set<UUID> memberIds = balances.stream()
                .map(MemberNet::getUserId)
                .collect(Collectors.toSet());

        validateRecordCommand(cmd, memberIds);

        Settlement settlement = Settlement.confirmed(groupId, cmd.payerId(), cmd.payeeId(), cmd.amount());
        return settlementRepository.save(settlement);
    }

    private void requireMember(UUID groupId, UUID userId, List<MemberNet> balances) {
        boolean isMember = balances.stream()
                .anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new GroupNotFoundException(groupId);
        }
    }

    private void validateRecordCommand(RecordSettlementCommand cmd, Set<UUID> memberIds) {
        if (cmd.amount() == null || cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (cmd.payerId() == null || cmd.payeeId() == null) {
            throw new IllegalArgumentException("Payer and payee must not be null");
        }
        if (cmd.payerId().equals(cmd.payeeId())) {
            throw new IllegalArgumentException("Payer and payee must be different");
        }
        if (!memberIds.contains(cmd.payerId())) {
            throw new IllegalArgumentException("Payer " + cmd.payerId() + " is not a group member");
        }
        if (!memberIds.contains(cmd.payeeId())) {
            throw new IllegalArgumentException("Payee " + cmd.payeeId() + " is not a group member");
        }
    }
}
