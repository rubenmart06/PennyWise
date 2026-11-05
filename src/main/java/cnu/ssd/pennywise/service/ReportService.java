package cnu.ssd.pennywise.service;

import cnu.ssd.pennywise.entity.TransactionEntity;
import cnu.ssd.pennywise.model.FinancialSummary;
import cnu.ssd.pennywise.model.TransactionType;
import cnu.ssd.pennywise.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the Reports screen: totals and
 * breakdowns over a requested period, optionally isolated to one member.
 */
@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * @param memberUserId when given, totals and the category breakdown are limited to that
     *                     member's own transactions; when {@code null},
     *                     the summary covers the whole household and includes a per-member breakdown
     */
    @Transactional(readOnly = true)
    public FinancialSummary getSummary(Long householdId, LocalDate fromDate, LocalDate toDate, Long memberUserId) {
        List<TransactionEntity> transactions = transactionRepository
                .search(householdId, fromDate, toDate, null, memberUserId, null);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<Long, String> memberNames = new LinkedHashMap<>();
        Map<Long, BigDecimal> memberIncome = new LinkedHashMap<>();
        Map<Long, BigDecimal> memberExpense = new LinkedHashMap<>();

        for (TransactionEntity t : transactions) {
            BigDecimal amount = t.getConvertedAmount();
            Long payerId = t.getPayer().getId();
            memberNames.putIfAbsent(payerId, t.getPayer().getUsername());

            if (t.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(amount);
                memberIncome.merge(payerId, amount, BigDecimal::add);
            } else {
                totalExpense = totalExpense.add(amount);
                memberExpense.merge(payerId, amount, BigDecimal::add);
                categoryTotals.merge(t.getCategoryName(), amount, BigDecimal::add);
            }
        }

        List<FinancialSummary.MemberTotal> memberTotals = new ArrayList<>();
        for (Long userId : memberNames.keySet()) {
            BigDecimal income = memberIncome.getOrDefault(userId, BigDecimal.ZERO);
            BigDecimal expense = memberExpense.getOrDefault(userId, BigDecimal.ZERO);
            memberTotals.add(new FinancialSummary.MemberTotal(userId, memberNames.get(userId), income, expense,
                    income.subtract(expense)));
        }

        List<FinancialSummary.CategoryTotal> categoryList = new ArrayList<>();
        categoryTotals.forEach((category, amount) -> categoryList.add(new FinancialSummary.CategoryTotal(category, amount)));

        return new FinancialSummary(totalIncome, totalExpense, totalIncome.subtract(totalExpense), memberTotals, categoryList);
    }
}
