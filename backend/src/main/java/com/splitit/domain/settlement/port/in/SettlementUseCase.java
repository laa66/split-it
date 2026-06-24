package com.splitit.domain.settlement.port.in;

import com.splitit.domain.settlement.model.Settlement;
import com.splitit.domain.settlement.model.SettlementSuggestion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SettlementUseCase {

    List<SettlementSuggestion> suggestPlan(UUID callerId, UUID groupId);

    Settlement recordSettlement(UUID callerId, UUID groupId, RecordSettlementCommand cmd);

    record RecordSettlementCommand(UUID payerId, UUID payeeId, BigDecimal amount) {}
}
