package com.splitit.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.util.UUID;

/** Spring Data projection for the live-calculated member balance query. */
public interface BalanceView {
    UUID getUserId();
    String getDisplayName();
    BigDecimal getBalance();
}
