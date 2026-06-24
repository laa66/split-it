package com.splitit.domain.settlement.service;

import com.splitit.domain.settlement.model.MemberNet;
import com.splitit.domain.settlement.model.SettlementSuggestion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy debt-simplification algorithm.
 * Works in integer cents to avoid floating-point rounding issues.
 * Guarantees at most n-1 transactions for n members.
 */
public class DebtSimplifier {

    // Balances with absolute value < MIN_CENT are noise (sub-cent rounding artifacts) and are ignored.
    private static final long MIN_CENT = 1L;

    private DebtSimplifier() {}

    public static List<SettlementSuggestion> simplify(List<MemberNet> balances) {
        // Convert to cents; filter out near-zero balances.
        List<MutableBalance> debtors = new ArrayList<>();
        List<MutableBalance> creditors = new ArrayList<>();

        for (MemberNet m : balances) {
            long cents = m.getBalance().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (cents <= -MIN_CENT) {
                debtors.add(new MutableBalance(m.getUserId(), m.getDisplayName(), -cents));
            } else if (cents >= MIN_CENT) {
                creditors.add(new MutableBalance(m.getUserId(), m.getDisplayName(), cents));
            }
        }

        // Sort descending by absolute amount for greedy efficiency.
        debtors.sort(Comparator.comparingLong(MutableBalance::cents).reversed());
        creditors.sort(Comparator.comparingLong(MutableBalance::cents).reversed());

        List<SettlementSuggestion> suggestions = new ArrayList<>();
        int d = 0;
        int c = 0;

        while (d < debtors.size() && c < creditors.size()) {
            MutableBalance debtor = debtors.get(d);
            MutableBalance creditor = creditors.get(c);

            long transfer = Math.min(debtor.cents(), creditor.cents());

            BigDecimal amount = BigDecimal.valueOf(transfer).movePointLeft(2);
            suggestions.add(new SettlementSuggestion(
                    debtor.userId(), debtor.name(),
                    creditor.userId(), creditor.name(),
                    amount));

            debtor.subtract(transfer);
            creditor.subtract(transfer);

            if (debtor.cents() < MIN_CENT) d++;
            if (creditor.cents() < MIN_CENT) c++;
        }

        return suggestions;
    }

    private static final class MutableBalance {
        private final java.util.UUID userId;
        private final String name;
        private long amount;

        MutableBalance(java.util.UUID userId, String name, long amount) {
            this.userId = userId;
            this.name = name;
            this.amount = amount;
        }

        java.util.UUID userId() { return userId; }
        String name() { return name; }
        long cents() { return amount; }
        void subtract(long v) { amount -= v; }
    }
}
