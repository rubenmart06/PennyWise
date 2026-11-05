package cnu.ssd.pennywise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * JPA entity backing the {@code exchange_rates} table: one static rate per
 * (household, foreign currency) pair, against that household's primary
 * currency.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exchange_rates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"household_id", "currency_code"})
})
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private HouseholdEntity household;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /** Units of {@code currencyCode} per 1 unit of the household's primary currency. */
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;
}
