package com.splitit.infrastructure.web.expense.dto;

import com.splitit.domain.expense.model.ExpenseShare;
import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseShareResponse(UUID userId, BigDecimal shareAmount) {

    public static ExpenseShareResponse from(ExpenseShare share) {
        return new ExpenseShareResponse(share.getUserId(), share.getShareAmount());
    }
}
