package com.splitit.infrastructure.web.expense.dto;

import com.splitit.domain.expense.model.MemberBalance;
import java.math.BigDecimal;
import java.util.UUID;

public record MemberBalanceResponse(UUID userId, String displayName, BigDecimal balance) {

    public static MemberBalanceResponse from(MemberBalance mb) {
        return new MemberBalanceResponse(mb.getUserId(), mb.getDisplayName(), mb.getBalance());
    }
}
