package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * One household member's share of a shared {@link Transaction}, in the
 * household's primary currency.
 */

@Data
@Getter
@AllArgsConstructor
public class SplitDetail {
    private final Long userId;
    private final String username;
    private final BigDecimal shareAmount;
}
