package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.settlement.model.MemberNet;
import com.splitit.domain.settlement.port.out.GroupBalanceProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GroupBalanceProviderAdapter implements GroupBalanceProvider {

    private final SpringDataExpenseRepository expenseRepository;

    public GroupBalanceProviderAdapter(SpringDataExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public List<MemberNet> currentBalances(UUID groupId) {
        return expenseRepository.calculateBalances(groupId).stream()
                .map(v -> new MemberNet(v.getUserId(), v.getDisplayName(), v.getBalance()))
                .toList();
    }
}
