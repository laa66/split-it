package com.splitit.domain.settlement.port.out;

import com.splitit.domain.settlement.model.MemberNet;
import java.util.List;
import java.util.UUID;

public interface GroupBalanceProvider {

    List<MemberNet> currentBalances(UUID groupId);
}
