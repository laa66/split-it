package com.splitit.domain.settlement.port.out;

import com.splitit.domain.settlement.model.Settlement;

public interface SettlementRepository {

    Settlement save(Settlement settlement);
}
