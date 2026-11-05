package cnu.ssd.pennywise.service;

import cnu.ssd.pennywise.entity.HouseholdEntity;
import cnu.ssd.pennywise.entity.SplitShareEmbeddable;
import cnu.ssd.pennywise.entity.TransactionEntity;
import cnu.ssd.pennywise.entity.UserEntity;
import cnu.ssd.pennywise.exception.TransactionException;
import cnu.ssd.pennywise.model.Debt;
import cnu.ssd.pennywise.model.TransactionScope;
import cnu.ssd.pennywise.model.TransactionType;
import cnu.ssd.pennywise.repository.HouseholdRepository;
import cnu.ssd.pennywise.repository.TransactionRepository;
import cnu.ssd.pennywise.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for debt settlement: aggregates every shared
 * transaction of a household into net per-member balances, then reduces those
 * balances to a minimal set of pairwise debts (the classic "simplify debts"
 * greedy algorithm). Settling a debt is recorded as a new shared transaction
 * so it folds into the same balance calculation going forward, rather than as
 * a separate kind of record.
 */
@Service
public class DebtSettlementService {

    private final TransactionRepository transactionRepository;
    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;

    public DebtSettlementService(TransactionRepository transactionRepository, HouseholdRepository householdRepository,
                                  UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.householdRepository = householdRepository;
        this.userRepository = userRepository;
    }

    /** The minimal set of "who owes whom" pairs that settles every shared transaction's balance. */
    @Transactional(readOnly = true)
    public List<Debt> getOptimizedDebts(Long householdId) {
        List<TransactionEntity> sharedTransactions = transactionRepository
                .findByHouseholdIdAndScopeFetchPayer(householdId, TransactionScope.SHARED);

        Map<Long, BigDecimal> balances = new HashMap<>();
        Map<Long, String> usernames = new HashMap<>();
        for (TransactionEntity t : sharedTransactions) {
            Long payerId = t.getPayer().getId();
            usernames.put(payerId, t.getPayer().getUsername());
            balances.merge(payerId, t.getConvertedAmount(), BigDecimal::add);
            for (SplitShareEmbeddable split : t.getSplits()) {
                balances.merge(split.getUserId(), split.getShareAmount().negate(), BigDecimal::add);
            }
        }
        if (!usernames.keySet().containsAll(balances.keySet())) {
            Map<Long, UserEntity> resolved = new HashMap<>();
            userRepository.findAllById(balances.keySet()).forEach(u -> resolved.put(u.getId(), u));
            resolved.forEach((id, u) -> usernames.putIfAbsent(id, u.getUsername()));
        }

        return optimize(balances, usernames);
    }

    /**
     * Records {@code fromUserId} paying {@code toUserId} {@code amount} as a
     * new shared transaction (payer = {@code fromUserId}, its only split =
     * {@code toUserId} for the full amount), which exactly offsets that pair's
     * balance going forward.
     *
     * @throws TransactionException if the household/users are unknown
     */
    @Transactional
    public void settleDebt(Long householdId, Long fromUserId, Long toUserId, BigDecimal amount) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new TransactionException("Unknown household."));
        UserEntity fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new TransactionException("Unknown user."));
        if (!userRepository.existsById(toUserId)) {
            throw new TransactionException("Unknown user.");
        }

        List<SplitShareEmbeddable> splits = List.of(new SplitShareEmbeddable(toUserId, amount));
        TransactionEntity settlement = new TransactionEntity();
        settlement.setHousehold(household);
        settlement.setType(TransactionType.EXPENSE);
        settlement.setDate(LocalDate.now());
        settlement.setCategory(null);
        settlement.setDescription("Settlement payment");
        settlement.setPayer(fromUser);
        settlement.setScope(TransactionScope.SHARED);
        settlement.setCurrencyCode(household.getPrimaryCurrency());
        settlement.setOriginalAmount(amount);
        settlement.setConvertedAmount(amount);
        settlement.setSplits(splits);
        transactionRepository.save(settlement);
    }

    /** Greedily matches the largest creditor with the largest debtor until every balance is ~zero. */
    private List<Debt> optimize(Map<Long, BigDecimal> balances, Map<Long, String> usernames) {
        List<Map.Entry<Long, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<Long, BigDecimal>> debtors = new ArrayList<>();
        BigDecimal epsilon = new BigDecimal("0.01");

        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().compareTo(epsilon) > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            } else if (entry.getValue().compareTo(epsilon.negate()) < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        debtors.sort((a, b) -> a.getValue().compareTo(b.getValue()));

        List<Debt> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < creditors.size() && j < debtors.size()) {
            Map.Entry<Long, BigDecimal> creditor = creditors.get(i);
            Map.Entry<Long, BigDecimal> debtor = debtors.get(j);
            BigDecimal owed = debtor.getValue().negate();
            BigDecimal settled = creditor.getValue().min(owed);

            result.add(new Debt(debtor.getKey(), usernames.get(debtor.getKey()),
                    creditor.getKey(), usernames.get(creditor.getKey()), settled));

            creditor.setValue(creditor.getValue().subtract(settled));
            debtor.setValue(debtor.getValue().add(settled));
            if (creditor.getValue().compareTo(epsilon) <= 0) {
                i++;
            }
            if (debtor.getValue().negate().compareTo(epsilon) <= 0) {
                j++;
            }
        }
        return result;
    }
}
