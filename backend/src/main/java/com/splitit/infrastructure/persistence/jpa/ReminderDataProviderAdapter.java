package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.group.model.GroupMember;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import com.splitit.domain.reminder.model.GroupBalanceSnapshot;
import com.splitit.domain.reminder.model.MemberLine;
import com.splitit.domain.reminder.port.out.ReminderDataProvider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReminderDataProviderAdapter implements ReminderDataProvider {

    private static final Logger log = LoggerFactory.getLogger(ReminderDataProviderAdapter.class);

    private final SpringDataGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SpringDataExpenseRepository expenseRepository;

    public ReminderDataProviderAdapter(SpringDataGroupRepository groupRepository,
                                       GroupMemberRepository groupMemberRepository,
                                       SpringDataExpenseRepository expenseRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public List<GroupBalanceSnapshot> snapshotAllGroups() {
        List<GroupIdNameView> groups = groupRepository.findAllIdAndName();
        List<GroupBalanceSnapshot> snapshots = new ArrayList<>();

        for (GroupIdNameView group : groups) {
            UUID groupId = group.getId();

            List<GroupMember> members = groupMemberRepository.findMembers(groupId);
            Map<UUID, String> emailByUserId = members.stream()
                    .collect(Collectors.toMap(GroupMember::userId, GroupMember::email));

            List<BalanceView> balances = expenseRepository.calculateBalances(groupId);

            List<MemberLine> lines = balances.stream()
                    .filter(b -> emailByUserId.containsKey(b.getUserId()))
                    .map(b -> new MemberLine(
                            emailByUserId.get(b.getUserId()),
                            b.getDisplayName(),
                            b.getBalance() != null ? b.getBalance() : BigDecimal.ZERO))
                    .toList();

            long debtors = lines.stream()
                    .filter(l -> l.balance().compareTo(BigDecimal.ZERO) < 0)
                    .count();
            log.info("Reminder snapshot: group '{}' ({} members, {} owing)",
                    group.getName(), lines.size(), debtors);

            snapshots.add(new GroupBalanceSnapshot(groupId, group.getName(), lines));
        }

        log.info("Reminder snapshot complete: {} group(s) scanned", snapshots.size());
        return snapshots;
    }
}
