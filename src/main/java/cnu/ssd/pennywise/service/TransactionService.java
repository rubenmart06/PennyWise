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

import cnu.ssd.pennywise.entity.HouseholdEntity;
import cnu.ssd.pennywise.entity.SplitShareEmbeddable;
import cnu.ssd.pennywise.entity.TransactionEntity;
import cnu.ssd.pennywise.entity.UserEntity;
import cnu.ssd.pennywise.exception.RateNotConfiguredException;
import cnu.ssd.pennywise.exception.TransactionException;
import cnu.ssd.pennywise.model.SplitDetail;
import cnu.ssd.pennywise.model.Transaction;
import cnu.ssd.pennywise.model.TransactionScope;
import cnu.ssd.pennywise.model.TransactionType;
import cnu.ssd.pennywise.repository.HouseholdRepository;
import cnu.ssd.pennywise.repository.MembershipRepository;
import cnu.ssd.pennywise.repository.TransactionRepository;
import cnu.ssd.pennywise.repository.UserRepository;
import cnu.ssd.pennywise.entity.MembershipEntity;
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

    public TransactionService(TransactionRepository transactionRepository, HouseholdRepository householdRepository,
                              MembershipRepository membershipRepository, UserRepository userRepository,
                              CurrencyService currencyService) {
        this.transactionRepository = transactionRepository;
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
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
                                         String category, String description, String currencyCode,
                                         BigDecimal originalAmount, TransactionScope scope,
                                         List<Long> sharedMemberUserIds, Map<Long, BigDecimal> percentageByUserId) {
        String normalizedCategory = category == null ? "" : category.trim();
        String normalizedCurrency = currencyCode == null ? "" : currencyCode.trim().toUpperCase();
        String normalizedDescription = description == null ? "" : description.trim();

        if (normalizedCategory.isEmpty()) {
            throw new TransactionException("Category is required.");
        }
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

        // May throw RateNotConfiguredException, deliberately left uncaught here so the
        // Controller can catch it specifically and prompt for a rate.
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

        TransactionEntity newTransactionEntity = new TransactionEntity();
        newTransactionEntity.setHousehold(household);
        newTransactionEntity.setType(type);
        newTransactionEntity.setDate(date);
        newTransactionEntity.setCategory(normalizedCategory);
        newTransactionEntity.setDescription(normalizedDescription);
        newTransactionEntity.setPayer(payer);
        newTransactionEntity.setScope(scope);
        newTransactionEntity.setCurrencyCode(normalizedCurrency);
        newTransactionEntity.setOriginalAmount(originalAmount);
        newTransactionEntity.setConvertedAmount(convertedAmount);
        newTransactionEntity.setSplits(splitEntities);
        transactionRepository.save(newTransactionEntity);

        return new Transaction(newTransactionEntity.getId(), type, date, normalizedCategory, normalizedDescription, payer.getId(),
                payer.getUsername(), scope, normalizedCurrency, originalAmount, convertedAmount, splitModels);
    }

    /** Every transaction of the given household, newest first, with payer and split usernames resolved. */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForHousehold(Long householdId) {
        List<TransactionEntity> entities = transactionRepository.findByHouseholdIdFetchPayer(householdId);

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
            result.add(new Transaction(entity.getId(), entity.getType(), entity.getDate(), entity.getCategory(),
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
