package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.group.model.GroupMember;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import com.splitit.domain.report.model.ReportBalance;
import com.splitit.domain.report.model.ReportExpense;
import com.splitit.domain.report.model.ReportSettlement;
import com.splitit.domain.report.port.out.ReportDataProvider;
import com.splitit.domain.settlement.model.MemberNet;
import com.splitit.domain.settlement.model.SettlementSuggestion;
import com.splitit.domain.settlement.port.out.GroupBalanceProvider;
import com.splitit.domain.settlement.service.DebtSimplifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Infra adapter for ReportDataProvider. Orchestrates cross-domain data assembly
 * (expenses + members + balance + settlement plan) entirely in infra — the domain/report
 * package never imports concrete classes from other domains.
 */
@Component
public class ReportDataProviderAdapter implements ReportDataProvider {

    private final SpringDataGroupRepository groupRepository;
    private final SpringDataGroupMemberRepository groupMemberJpaRepository;
    private final SpringDataExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupBalanceProvider groupBalanceProvider;

    public ReportDataProviderAdapter(SpringDataGroupRepository groupRepository,
                                     SpringDataGroupMemberRepository groupMemberJpaRepository,
                                     SpringDataExpenseRepository expenseRepository,
                                     GroupMemberRepository groupMemberRepository,
                                     GroupBalanceProvider groupBalanceProvider) {
        this.groupRepository = groupRepository;
        this.groupMemberJpaRepository = groupMemberJpaRepository;
        this.expenseRepository = expenseRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupBalanceProvider = groupBalanceProvider;
    }

    @Override
    public boolean isGroupMember(UUID groupId, UUID userId) {
        return groupMemberJpaRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public Optional<String> findGroupName(UUID groupId) {
        return groupRepository.findById(groupId).map(GroupJpaEntity::getName);
    }

    @Override
    public List<ReportExpense> expensesInRange(UUID groupId, LocalDate from, LocalDate to) {
        return expenseRepository.findExpensesInRange(groupId, from, to).stream()
                .map(v -> new ReportExpense(
                        v.getExpenseDate(),
                        v.getTitle(),
                        v.getPaidByName(),
                        v.getAmount(),
                        v.getSplitType()))
                .toList();
    }

    @Override
    public List<ReportBalance> balances(UUID groupId) {
        return expenseRepository.calculateBalances(groupId).stream()
                .map(v -> new ReportBalance(
                        v.getDisplayName(),
                        v.getBalance() != null ? v.getBalance() : BigDecimal.ZERO))
                .toList();
    }

    @Override
    public List<ReportSettlement> settlementPlan(UUID groupId) {
        List<MemberNet> nets = groupBalanceProvider.currentBalances(groupId);
        List<SettlementSuggestion> suggestions = DebtSimplifier.simplify(nets);

        Map<UUID, String> nameByUserId = groupMemberRepository.findMembers(groupId).stream()
                .collect(Collectors.toMap(GroupMember::userId, GroupMember::displayName));

        return suggestions.stream()
                .map(s -> new ReportSettlement(
                        nameByUserId.getOrDefault(s.getPayerId(), s.getPayerName()),
                        nameByUserId.getOrDefault(s.getPayeeId(), s.getPayeeName()),
                        s.getAmount()))
                .toList();
    }
}
