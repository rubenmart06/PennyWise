package cnu.ssd.pennywise.entity;

import cnu.ssd.pennywise.model.HouseholdKind;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity backing the {@code households} table. Membership rows reference
 * this entity from {@link MembershipEntity}; this class deliberately does not
 * hold a {@code @OneToMany} collection back to them, {@code MembershipRepository}
 * is the single place that loads a household's members.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "households")
public class HouseholdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HouseholdKind kind;

    @Column(name = "primary_currency", nullable = false, length = 3)
    private String primaryCurrency;

    /** Household-wide monthly budget limit; null means none set. */
    @Column(name = "global_budget_limit", precision = 12, scale = 2)
    private java.math.BigDecimal globalBudgetLimit;
}
