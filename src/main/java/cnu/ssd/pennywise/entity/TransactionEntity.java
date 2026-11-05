package cnu.ssd.pennywise.entity;

import cnu.ssd.pennywise.model.TransactionScope;
import cnu.ssd.pennywise.model.TransactionType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity backing the {@code transactions} table.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class TransactionEntity {

    public static final String SETTLEMENT_LABEL = "Debt settlement";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private HouseholdEntity household;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private UserEntity payer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionScope scope;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "converted_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal convertedAmount;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "transaction_splits", joinColumns = @JoinColumn(name = "transaction_id"))
    private List<SplitShareEmbeddable> splits = new ArrayList<>();

    /** Display name of the category; settlement records (no category) read "Debt settlement". */
    public String getCategoryName() {
        return category == null ? SETTLEMENT_LABEL : category.getName();
    }
}
