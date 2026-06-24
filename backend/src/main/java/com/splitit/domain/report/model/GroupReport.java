package com.splitit.domain.report.model;

import java.time.LocalDate;
import java.util.List;

public record GroupReport(
        String groupName,
        LocalDate from,
        LocalDate to,
        List<ReportExpense> expenses,
        List<ReportBalance> balances,
        List<ReportSettlement> settlements) {
}
