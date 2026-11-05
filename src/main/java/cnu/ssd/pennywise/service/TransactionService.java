package cnu.ssd.pennywise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cnu.ssd.pennywise.entity.*;
import cnu.ssd.pennywise.exception.RateNotConfiguredException;
import cnu.ssd.pennywise.exception.TransactionException;
import cnu.ssd.pennywise.model.*;
import cnu.ssd.pennywise.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for creating and listing financial records:
 * CRUD on transactions, individual-vs-shared tagging, equal/percentage splits.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final HouseholdRepository householdRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;

    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, HouseholdRepository householdRepository,
                              MembershipRepository membershipRepository, UserRepository userRepository,
                              CurrencyService currencyService, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Validates and persists a new record, converting its amount into the
     * household's primary currency and (for shared records) computing each
     * selected member's share.
     *
     * @param sharedMemberUserIds who the expense is split with; ignored unless {@code scope == SHARED}
     * @param percentageByUserId  each member's percentage (0–100, must sum to ~100); {@code null}/empty splits equally instead
     * @throws TransactionException       on invalid input, or an unknown household/payer
     * @throws RateNotConfiguredException if {@code currencyCode} needs a static rate that isn't set yet
     */
    @Transactional
    public Transaction createTransaction(Long householdId, Long payerUserId, TransactionType type, LocalDate date,
                                         Long categoryId, String description, String currencyCode,
                                         BigDecimal originalAmount, TransactionScope scope,
                                         List<Long> sharedMemberUserIds, Map<Long, BigDecimal> percentageByUserId) {
        String normalizedCurrency = currencyCode == null ? "" : currencyCode.trim().toUpperCase();
        String normalizedDescription = description == null ? "" : description.trim();

        if (date == null) {
            throw new TransactionException("Date is required.");
        }
        if (originalAmount == null || originalAmount.signum() <= 0) {
            throw new TransactionException("Amount must be a positive number.");
        }
        if (normalizedCurrency.length() != 3) {
            throw new TransactionException("Currency must be a 3-letter code.");
        }

        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new TransactionException("Unknown household."));
        UserEntity payer = userRepository.findById(payerUserId)
                .orElseThrow(() -> new TransactionException("Unknown payer."));
        if (!membershipRepository.existsByHouseholdAndUser(household, payer)) {
            throw new TransactionException("The payer must be a member of this household.");
        }
        CategoryEntity category = resolveCategory(householdId, categoryId, type);

        // May throw RateNotConfiguredException \u2014 deliberately left uncaught here so the
        // Controller can catch it specifically and prompt for a rate (use case V.5, alt path 4.1).
        BigDecimal convertedAmount = currencyService.convertToPrimary(householdId, normalizedCurrency, originalAmount);

        List<SplitShareEmbeddable> splitEntities = new ArrayList<>();
        List<SplitDetail> splitModels = new ArrayList<>();

        if (scope == TransactionScope.SHARED) {
            if (sharedMemberUserIds == null || sharedMemberUserIds.isEmpty()) {
                throw new TransactionException("Select at least one member to split a shared expense with.");
            }
            Map<Long, UserEntity> membersById = membershipRepository.findByHousehold(household).stream()
                    .collect(Collectors.toMap(m -> m.getUser().getId(), MembershipEntity::getUser));
            for (Long userId : sharedMemberUserIds) {
                if (!membersById.containsKey(userId)) {
                    throw new TransactionException("Every split member must belong to this household.");
                }
            }

            Map<Long, BigDecimal> shares = computeShares(convertedAmount, sharedMemberUserIds, percentageByUserId);
            for (Map.Entry<Long, BigDecimal> entry : shares.entrySet()) {
                splitEntities.add(new SplitShareEmbeddable(entry.getKey(), entry.getValue()));
                UserEntity member = membersById.get(entry.getKey());
                splitModels.add(new SplitDetail(member.getId(), member.getUsername(), entry.getValue()));
            }
        }

        TransactionEntity entity = new TransactionEntity();
        entity.setHousehold(household);
        entity.setType(type);
        entity.setDate(date);
        entity.setCategory(category);
        entity.setDescription(normalizedDescription);
        entity.setPayer(payer);
        entity.setScope(scope);
        entity.setCurrencyCode(normalizedCurrency);
        entity.setOriginalAmount(originalAmount);
        entity.setConvertedAmount(convertedAmount);
        entity.setSplits(splitEntities);
        transactionRepository.save(entity);

        return new Transaction(entity.getId(), type, date, category.getId(), category.getName(), normalizedDescription, payer.getId(),
                payer.getUsername(), scope, normalizedCurrency, originalAmount, convertedAmount, splitModels);
    }

    /** Every transaction of the given household, newest first, with payer and split usernames resolved. */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForHousehold(Long householdId) {
        return toModels(transactionRepository.findByHouseholdIdFetchPayer(householdId));
    }

    /** Filtered/search lookup, with payer and split usernames resolved. */
    @Transactional(readOnly = true)
    public List<Transaction> searchTransactions(Long householdId, TransactionFilter filter) {
        TransactionFilter f = filter == null ? TransactionFilter.none() : filter;
        List<TransactionEntity> entities = transactionRepository.search(householdId, f.getFromDate(), f.getToDate(),
                f.getCategory(), f.getMemberUserId(), f.getKeyword());
        return toModels(entities);
    }

    /**
     * Updates an existing record in place, re-validating and re-converting the
     * amount exactly as {@link #createTransaction} does.
     *
     * @throws TransactionException       on invalid input, or if the record/payer is unknown
     * @throws RateNotConfiguredException if {@code currencyCode} needs a static rate that isn't set yet
     */
    @Transactional
    public Transaction updateTransaction(Long transactionId, Long payerUserId, TransactionType type, LocalDate date,
                                         Long categoryId, String description, String currencyCode,
                                         BigDecimal originalAmount, TransactionScope scope,
                                         List<Long> sharedMemberUserIds, Map<Long, BigDecimal> percentageByUserId) {
        TransactionEntity entity = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException("Unknown transaction."));
        HouseholdEntity household = entity.getHousehold();

        String normalizedCurrency = currencyCode == null ? "" : currencyCode.trim().toUpperCase();
        String normalizedDescription = description == null ? "" : description.trim();

        if (date == null) {
            throw new TransactionException("Date is required.");
        }
        if (originalAmount == null || originalAmount.signum() <= 0) {
            throw new TransactionException("Amount must be a positive number.");
        }
        if (normalizedCurrency.length() != 3) {
            throw new TransactionException("Currency must be a 3-letter code.");
        }

        UserEntity payer = userRepository.findById(payerUserId)
                .orElseThrow(() -> new TransactionException("Unknown payer."));
        if (!membershipRepository.existsByHouseholdAndUser(household, payer)) {
            throw new TransactionException("The payer must be a member of this household.");
        }
        CategoryEntity category = resolveCategory(household.getId(), categoryId, type);

        BigDecimal convertedAmount = currencyService.convertToPrimary(household.getId(), normalizedCurrency, originalAmount);

        List<SplitShareEmbeddable> splitEntities = new ArrayList<>();
        List<SplitDetail> splitModels = new ArrayList<>();
        if (scope == TransactionScope.SHARED) {
            if (sharedMemberUserIds == null || sharedMemberUserIds.isEmpty()) {
                throw new TransactionException("Select at least one member to split a shared expense with.");
            }
            Map<Long, UserEntity> membersById = membershipRepository.findByHousehold(household).stream()
                    .collect(Collectors.toMap(m -> m.getUser().getId(), MembershipEntity::getUser));
            for (Long userId : sharedMemberUserIds) {
                if (!membersById.containsKey(userId)) {
                    throw new TransactionException("Every split member must belong to this household.");
                }
            }
            Map<Long, BigDecimal> shares = computeShares(convertedAmount, sharedMemberUserIds, percentageByUserId);
            for (Map.Entry<Long, BigDecimal> entry : shares.entrySet()) {
                splitEntities.add(new SplitShareEmbeddable(entry.getKey(), entry.getValue()));
                UserEntity member = membersById.get(entry.getKey());
                splitModels.add(new SplitDetail(member.getId(), member.getUsername(), entry.getValue()));
            }
        }

        entity.setType(type);
        entity.setDate(date);
        entity.setCategory(category);
        entity.setDescription(normalizedDescription);
        entity.setPayer(payer);
        entity.setScope(scope);
        entity.setCurrencyCode(normalizedCurrency);
        entity.setOriginalAmount(originalAmount);
        entity.setConvertedAmount(convertedAmount);
        entity.setSplits(splitEntities);

        return new Transaction(entity.getId(), type, date, category.getId(), category.getName(), normalizedDescription, payer.getId(),
                payer.getUsername(), scope, normalizedCurrency, originalAmount, convertedAmount, splitModels);
    }

    /** @throws TransactionException if the record is unknown */
    @Transactional
    public void deleteTransaction(Long transactionId) {
        if (!transactionRepository.existsById(transactionId)) {
            throw new TransactionException("Unknown transaction.");
        }
        transactionRepository.deleteById(transactionId);
    }

    /** Loads the chosen category and checks it belongs to the household and matches the record's type. */
    private CategoryEntity resolveCategory(Long householdId, Long categoryId, TransactionType type) {
        if (categoryId == null) {
            throw new TransactionException("Category is required.");
        }
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new TransactionException("Unknown category."));
        if (!category.getHousehold().getId().equals(householdId)) {
            throw new TransactionException("That category belongs to a different household.");
        }
        if (category.getType() != type) {
            throw new TransactionException("The category's type (Income/Expense) must match the record's type.");
        }
        return category;
    }

    private List<Transaction> toModels(List<TransactionEntity> entities) {
        Set<Long> splitUserIds = entities.stream()
                .flatMap(t -> t.getSplits().stream())
                .map(SplitShareEmbeddable::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> usernameById = splitUserIds.isEmpty() ? Map.of()
                : userRepository.findAllById(splitUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername));

        List<Transaction> result = new ArrayList<>(entities.size());
        for (TransactionEntity entity : entities) {
            List<SplitDetail> splits = entity.getSplits().stream()
                    .map(s -> new SplitDetail(s.getUserId(), usernameById.get(s.getUserId()), s.getShareAmount()))
                    .collect(Collectors.toList());
            result.add(new Transaction(entity.getId(), entity.getType(), entity.getDate(),
                    entity.getCategory() == null ? null : entity.getCategory().getId(), entity.getCategoryName(),
                    entity.getDescription(), entity.getPayer().getId(), entity.getPayer().getUsername(),
                    entity.getScope(), entity.getCurrencyCode(), entity.getOriginalAmount(),
                    entity.getConvertedAmount(), splits));
        }
        return result;
    }

    /**
     * Splits {@code totalAmount} across {@code memberUserIds}, either equally or
     * by the given percentages. Whichever member is processed last absorbs the
     * rounding remainder, so the shares always sum to exactly {@code totalAmount}.
     */
    private Map<Long, BigDecimal> computeShares(BigDecimal totalAmount, List<Long> memberUserIds,
                                                Map<Long, BigDecimal> percentageByUserId) {
        Map<Long, BigDecimal> shares = new LinkedHashMap<>();

        if (percentageByUserId == null || percentageByUserId.isEmpty()) {
            BigDecimal count = BigDecimal.valueOf(memberUserIds.size());
            BigDecimal each = totalAmount.divide(count, 2, RoundingMode.DOWN);
            BigDecimal remainder = totalAmount.subtract(each.multiply(count));
            for (int i = 0; i < memberUserIds.size(); i++) {
                BigDecimal amount = (i == memberUserIds.size() - 1) ? each.add(remainder) : each;
                shares.put(memberUserIds.get(i), amount);
            }
        } else {
            BigDecimal totalPercentage = percentageByUserId.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalPercentage.subtract(BigDecimal.valueOf(100)).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw new TransactionException("Split percentages must add up to 100%.");
            }
            BigDecimal distributed = BigDecimal.ZERO;
            for (int i = 0; i < memberUserIds.size(); i++) {
                Long userId = memberUserIds.get(i);
                BigDecimal amount;
                if (i == memberUserIds.size() - 1) {
                    amount = totalAmount.subtract(distributed);
                } else {
                    BigDecimal percentage = percentageByUserId.getOrDefault(userId, BigDecimal.ZERO);
                    amount = totalAmount.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    distributed = distributed.add(amount);
                }
                shares.put(userId, amount);
            }
        }
        return shares;
    }
}
