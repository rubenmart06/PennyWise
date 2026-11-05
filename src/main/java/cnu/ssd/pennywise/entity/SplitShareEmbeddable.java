package cnu.ssd.pennywise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Embeddable value object (not its own {@code @Entity}/repository): one
 * household member's share of a shared {@link TransactionEntity}, stored in
 * the {@code transaction_splits} table via {@code @ElementCollection}. Holds
 * only the member's {@code userId}, not a JPA relation, so reading it never
 * risks a lazy-loading exception.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SplitShareEmbeddable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "share_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;
}
