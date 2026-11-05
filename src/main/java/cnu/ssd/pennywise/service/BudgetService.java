package cnu.ssd.pennywise.service;

import cnu.ssd.pennywise.entity.CategoryEntity;
import cnu.ssd.pennywise.entity.HouseholdEntity;
import cnu.ssd.pennywise.entity.TransactionEntity;
import cnu.ssd.pennywise.exception.BudgetException;
import cnu.ssd.pennywise.model.BudgetLimit;
import cnu.ssd.pennywise.model.Category;
import cnu.ssd.pennywise.model.TransactionType;
import cnu.ssd.pennywise.repository.CategoryRepository;
import cnu.ssd.pennywise.repository.HouseholdRepository;
import cnu.ssd.pennywise.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for categories and budget limits:
 * create/edit/delete categories, set the household-wide limit, and warn when
 * an expense would exceed the relevant limit.
 */
@Service
public class BudgetService {

    private final CategoryRepository categoryRepository;
    private final HouseholdRepository householdRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(CategoryRepository categoryRepository, HouseholdRepository householdRepository,
                          TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.householdRepository = householdRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories(Long householdId) {
        return categoryRepository.findByHousehold_Id(householdId).stream().map(this::toModel).collect(Collectors.toList());
    }

    /** @throws BudgetException if the name is blank/duplicate within the household, or the limit is negative */
    @Transactional
    public Category createCategory(Long householdId, String name, TransactionType type, BigDecimal limitAmount) {
        String normalizedName = validateNameAndLimit(householdId, name, limitAmount, null);
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new BudgetException("Unknown household."));
        CategoryEntity newCategoryEntity = new CategoryEntity();
        newCategoryEntity.setHousehold(household);
        newCategoryEntity.setName(normalizedName);
        newCategoryEntity.setType(type);
        newCategoryEntity.setLimitAmount(limitAmount);
        CategoryEntity entity = categoryRepository.save(newCategoryEntity);
        return toModel(entity);
    }

    @Transactional
    public Category updateCategory(Long categoryId, String name, TransactionType type, BigDecimal limitAmount) {
        CategoryEntity entity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BudgetException("Unknown category."));
        String normalizedName = validateNameAndLimit(entity.getHousehold().getId(), name, limitAmount, categoryId);
        if (entity.getType() != type && transactionRepository.existsByCategory_Id(categoryId)) {
            throw new BudgetException("This category already has transactions, so its type can't be changed.");
        }
        entity.setName(normalizedName);
        entity.setType(type);
        entity.setLimitAmount(limitAmount);
        return toModel(entity);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BudgetException("Unknown category.");
        }
        if (transactionRepository.existsByCategory_Id(categoryId)) {
            throw new BudgetException("This category is used by existing transactions — delete or edit those first.");
        }
        categoryRepository.deleteById(categoryId);
    }

    /** @throws BudgetException if the limit is negative */
    @Transactional
    public void setGlobalBudgetLimit(Long householdId, BigDecimal limitAmount) {
        if (limitAmount != null && limitAmount.signum() < 0) {
            throw new BudgetException("The global limit must be zero or positive.");
        }
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new BudgetException("Unknown household."));
        household.setGlobalBudgetLimit(limitAmount);
    }

    /**
     * The global limit (if set) plus every category's limit, each paired with
     * how much has been spent against it so far this calendar month.
     */
    @Transactional(readOnly = true)
    public List<BudgetLimit> getBudgetOverview(Long householdId) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new BudgetException("Unknown household."));
        List<CategoryEntity> categories = categoryRepository.findByHousehold_Id(householdId);

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        List<TransactionEntity> monthExpenses = transactionRepository
                .search(householdId, monthStart, monthEnd, null, null, null).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.toList());

        Map<String, BigDecimal> spentByCategory = new HashMap<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (TransactionEntity t : monthExpenses) {
            spentByCategory.merge(t.getCategoryName(), t.getConvertedAmount(), BigDecimal::add);
            totalSpent = totalSpent.add(t.getConvertedAmount());
        }

        List<BudgetLimit> result = new ArrayList<>();
        if (household.getGlobalBudgetLimit() != null) {
            result.add(new BudgetLimit("Global limit", household.getGlobalBudgetLimit(), totalSpent));
        }
        for (CategoryEntity category : categories) {
            if (category.getLimitAmount() != null) {
                BigDecimal spent = spentByCategory.getOrDefault(category.getName(), BigDecimal.ZERO);
                result.add(new BudgetLimit(category.getName(), category.getLimitAmount(), spent));
            }
        }
        return result;
    }

    /**
     * Checks a prospective expense against its category's limit and the
     * household's global limit, each evaluated over the
     * current month including the new amount.
     *
     * @return a warning message if either limit would be exceeded, or {@code null} if not
     */
    @Transactional(readOnly = true)
    public String checkLimitExceeded(Long householdId, String category, BigDecimal newExpenseConvertedAmount) {
        List<BudgetLimit> overview = getBudgetOverview(householdId);
        for (BudgetLimit limit : overview) {
            boolean matchesGlobal = limit.getLabel().equals("Global limit");
            boolean matchesCategory = limit.getLabel().equals(category);
            if ((matchesGlobal || matchesCategory) && limit.hasLimit()) {
                BigDecimal projected = limit.getSpentAmount().add(newExpenseConvertedAmount);
                if (projected.compareTo(limit.getLimitAmount()) > 0) {
                    return (matchesGlobal ? "This expense exceeds the household's global monthly limit."
                            : "This expense exceeds the monthly limit for \"" + category + "\".");
                }
            }
        }
        return null;
    }

    private String validateNameAndLimit(Long householdId, String name, BigDecimal limitAmount, Long excludingCategoryId) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new BudgetException("Category name is required.");
        }
        if (limitAmount != null && limitAmount.signum() < 0) {
            throw new BudgetException("The budget limit must be zero or positive.");
        }
        categoryRepository.findByHousehold_IdAndName(householdId, normalizedName).ifPresent(existing -> {
            if (!existing.getId().equals(excludingCategoryId)) {
                throw new BudgetException("That category name is already used.");
            }
        });
        return normalizedName;
    }

    private Category toModel(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName(), entity.getType(), entity.getLimitAmount());
    }
}
