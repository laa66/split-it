package com.splitit.domain.report.service;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.report.model.GroupReport;
import com.splitit.domain.report.port.in.GenerateReportUseCase;
import com.splitit.domain.report.port.out.ReportDataProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pure domain service — no Spring annotations.
 * Authorization enforced here: non-member → GroupNotFoundException (no existence leak).
 */
public class ReportService implements GenerateReportUseCase {

    static final LocalDate EPOCH = LocalDate.of(1970, 1, 1);

    private final ReportDataProvider dataProvider;
    private final Clock clock;

    public ReportService(ReportDataProvider dataProvider, Clock clock) {
        this.dataProvider = dataProvider;
        this.clock = clock;
    }

    @Override
    public GroupReport generate(UUID callerId, UUID groupId, LocalDate from, LocalDate to) {
        if (!dataProvider.isGroupMember(groupId, callerId)) {
            throw new GroupNotFoundException(groupId);
        }

        String groupName = dataProvider.findGroupName(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        LocalDate effectiveFrom = from != null ? from : EPOCH;
        LocalDate effectiveTo = to != null ? to : LocalDate.now(clock);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException(
                    "'from' date must not be after 'to' date");
        }

        return new GroupReport(
                groupName,
                effectiveFrom,
                effectiveTo,
                dataProvider.expensesInRange(groupId, effectiveFrom, effectiveTo),
                dataProvider.balances(groupId),
                dataProvider.settlementPlan(groupId));
    }
}
