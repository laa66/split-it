package com.splitit.domain.report.port.in;

import com.splitit.domain.report.model.GroupReport;
import java.time.LocalDate;
import java.util.UUID;

public interface GenerateReportUseCase {

    /**
     * Generates a group report for the given caller.
     * Throws GroupNotFoundException if the caller is not a group member (no existence leak).
     * Null from/to are replaced with sensible defaults (epoch / today).
     */
    GroupReport generate(UUID callerId, UUID groupId, LocalDate from, LocalDate to);
}
