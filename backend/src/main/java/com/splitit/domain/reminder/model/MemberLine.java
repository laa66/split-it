package com.splitit.domain.reminder.model;

import java.math.BigDecimal;

public record MemberLine(String email, String displayName, BigDecimal balance) {
}
