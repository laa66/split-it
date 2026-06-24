package com.splitit.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Spring Data projection for the report expense query (includes paidBy display name). */
public interface ReportExpenseView {
    LocalDate getExpenseDate();
    String getTitle();
    String getPaidByName();
    BigDecimal getAmount();
    String getSplitType();
}
