package com.splitit.domain.expense.port.in;

import com.splitit.domain.expense.model.MemberBalance;
import java.util.List;
import java.util.UUID;

public interface CalculateBalanceUseCase {

    /**
     * Returns live-calculated balances for all members of the group.
     * Caller must be a member.
     *
     * @throws com.splitit.domain.group.exception.GroupNotFoundException if not a member
     */
    List<MemberBalance> balance(UUID callerId, UUID groupId);
}
