package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** Aggregated income/expense/net totals for a household over a period, globally and broken down. */

@Data
@Getter
@AllArgsConstructor
public class FinancialSummary {

    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal netBalance;
    private final List<MemberTotal> memberTotals;
    private final List<CategoryTotal> categoryTotals;

    /** One member's income/expense/net for the period. */
    public static final class MemberTotal {
        private final Long userId;
        private final String username;
        private final BigDecimal income;
        private final BigDecimal expense;
        private final BigDecimal net;

        public MemberTotal(Long userId, String username, BigDecimal income, BigDecimal expense, BigDecimal net) {
            this.userId = userId;
            this.username = username;
            this.income = income;
            this.expense = expense;
            this.net = net;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public BigDecimal getIncome() {
            return income;
        }

        public BigDecimal getExpense() {
            return expense;
        }

        public BigDecimal getNet() {
            return net;
        }
    }

    /** Total expenses for one category over the period (for the Reports pie chart). */
    public static final class CategoryTotal {
        private final String category;
        private final BigDecimal amount;

        public CategoryTotal(String category, BigDecimal amount) {
            this.category = category;
            this.amount = amount;
        }

        public String getCategory() {
            return category;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
