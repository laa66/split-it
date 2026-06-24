package com.splitit.domain.report.port.out;

import com.splitit.domain.report.model.ReportBalance;
import com.splitit.domain.report.model.ReportExpense;
import com.splitit.domain.report.model.ReportSettlement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportDataProvider {

    boolean isGroupMember(UUID groupId, UUID userId);

    Optional<String> findGroupName(UUID groupId);

    List<ReportExpense> expensesInRange(UUID groupId, LocalDate from, LocalDate to);

    List<ReportBalance> balances(UUID groupId);

    List<ReportSettlement> settlementPlan(UUID groupId);
}
